package com.finance.token.route

import com.finance.token.processor.KafkaPublisherProcessor
import com.finance.token.processor.TokenExchangeProcessor
import com.finance.token.processor.TokenFormProcessor
import com.finance.token.processor.TokenParseProcessor
import com.finance.token.processor.TransactionFetcherProcessor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode
import org.slf4j.LoggerFactory

@ApplicationScoped
class TokenRoute : RouteBuilder() {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenRoute::class.java)
    }

    @Inject
    lateinit var tokenExchangeProcessor: TokenExchangeProcessor

    @Inject
    lateinit var tokenFormProcessor: TokenFormProcessor

    @Inject
    lateinit var tokenParseProcessor: TokenParseProcessor

    @Inject
    lateinit var transactionFetcherProcessor: TransactionFetcherProcessor

    @Inject
    lateinit var kafkaPublisherProcessor: KafkaPublisherProcessor

    override fun configure() {
        // Error handling
        onException(IllegalArgumentException::class.java)
            .handled(true)
            .logHandled(true)
            .log("[400] Bad request: ${'$'}{exception.message}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(400)
            .setBody().simple("{\"error\":\"${'$'}{exception.message}\"}")

        onException(java.net.ConnectException::class.java)
            .handled(true)
            .logHandled(true)
            .log("[502] Service unavailable: ${'$'}{exception.message}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(502)
            .setBody().simple("{\"error\":\"Upstream service unavailable\"}")

        onException(org.apache.camel.http.base.HttpOperationFailedException::class.java)
            .handled(true)
            .logHandled(true)
            .process { ex ->
                val hofe = ex.getProperty(Exchange.EXCEPTION_CAUGHT, org.apache.camel.http.base.HttpOperationFailedException::class.java)
                val status = hofe?.statusCode ?: 500
                val raw = hofe?.responseBody ?: ""
                val mapped = when (status) {
                    400 -> 400
                    401, 403 -> 401
                    in 500..599 -> 503
                    else -> 500
                }
                ex.message.setHeader(Exchange.HTTP_RESPONSE_CODE, mapped)
                val safe = raw.replace("\"", "\\\"").take(1000)
                ex.message.body = """{"error":"Upstream HTTP $status","details":"$safe"}"""
            }

        onException(Exception::class.java)
            .handled(true)
            .logHandled(true)
            .log("[500] Unexpected error: ${'$'}{exception}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(500)
            .setBody().simple("{\"error\":\"Internal server error\"}")

        // REST DSL backed by Quarkus HTTP (platform-http)
        restConfiguration()
            .component("platform-http")
            .bindingMode(RestBindingMode.json)

        rest("/")
            .get("token")
                .description("Exchange auth code for token, fetch transactions, and publish to Kafka")
                .produces("application/json")
                .param().name("code").type(org.apache.camel.model.rest.RestParamType.query).required(true).dataType("string").endParam()
                .to("direct:token-orchestrate")

        from("direct:token-orchestrate")
            .id("token-orchestration-route")
            .log("Received /token request with code=${'$'}{header.code}")
            // Validate 'code' presence and format (basic)
            .choice()
                .`when`().simple("${'$'}{header.code} == null || ${'$'}{header.code} == ''")
                    .throwException(IllegalArgumentException("Missing 'code' query parameter"))
            .end()
            .process { ex ->
                val code = ex.message.getHeader("code", String::class.java) ?: ""
                val ok = code.length in 8..512 && code.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
                if (!ok) throw IllegalArgumentException("Invalid 'code' format")
            }

            // 1) Exchange code for JWT access token (with retry)
            .to("direct:exchange-token")
            .log("Token exchange successful for code=${'$'}{header.code}")

            // 2) Fetch transactions from transactions-service (with retry)
            .to("direct:fetch-transactions")
            .log("Fetched transactions for user from transactions-service")

            // 3) Build Kafka message and publish with DLQ fallback
            .doTry()
                .process(kafkaPublisherProcessor)
                .toD("${'$'}{properties:kafka.publish.endpoint}")
                .log("Published to endpoint=${'$'}{properties:kafka.publish.endpoint}")
            .doCatch(Exception::class.java)
                .log("Kafka publish failed: ${'$'}{exception.message}. Routing to DLQ...")
                .setHeader("x-publish-error").simple("${'$'}{exception.message}")
                .toD("${'$'}{properties:kafka.publish.dlq.endpoint}")
                .log("Published to DLQ endpoint=${'$'}{properties:kafka.publish.dlq.endpoint}")
            .end()

            // 4) Respond to client with an acknowledgment
            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(200)
            .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
            .setBody().simple("{\"status\":\"ok\",\"message\":\"Transactions published\"}")

        from("direct:exchange-token")
            .routeId("exchange-token-route")
            .errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(200)
                .useExponentialBackOff()
                .backOffMultiplier(2.0))
            .process(tokenFormProcessor)
            // Clean inbound HTTP headers so camel-http doesn't reuse /token path
            .removeHeaders("CamelHttp*")
            .removeHeader(Exchange.HTTP_URI)
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_QUERY)
            // Call Keycloak token endpoint
            .toD("{{keycloak.token.url}}?bridgeEndpoint=true&throwExceptionOnFailure=true")
            .process(tokenParseProcessor)

        from("direct:fetch-transactions")
            .routeId("fetch-transactions-route")
            .errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(200)
                .useExponentialBackOff()
                .backOffMultiplier(2.0))
            .process(transactionFetcherProcessor)
    }
}
