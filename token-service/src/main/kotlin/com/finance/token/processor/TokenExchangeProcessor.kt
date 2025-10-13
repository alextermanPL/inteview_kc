package com.finance.token.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.ProducerTemplate
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ApplicationScoped
class TokenExchangeProcessor : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenExchangeProcessor::class.java)
    }

    @Inject
    lateinit var producerTemplate: ProducerTemplate

    @Inject
    lateinit var objectMapper: ObjectMapper

    @ConfigProperty(name = "keycloak.token.url")
    lateinit var tokenUrl: String

    @ConfigProperty(name = "keycloak.client.id")
    lateinit var clientId: String

    @ConfigProperty(name = "keycloak.redirect.uri", defaultValue = "http://localhost:8081/token")
    lateinit var redirectUri: String

    @ConfigProperty(name = "keycloak.client.secret")
    lateinit var clientSecretOpt: java.util.Optional<String>

    override fun process(exchange: Exchange) {
        val code = exchange.message.getHeader("code", String::class.java)
            ?: throw IllegalArgumentException("Missing 'code' query parameter")

        logger.info("Exchanging authorization code for token")

        val form = buildFormBody(code)
        val headers = mutableMapOf<String, Any>(
            Exchange.HTTP_METHOD to "POST",
            Exchange.CONTENT_TYPE to "application/x-www-form-urlencoded",
            "Accept" to "application/json"
        )

        // Perform HTTP POST to Keycloak token endpoint
        val endpoint = if (tokenUrl.startsWith("http")) "$tokenUrl?throwExceptionOnFailure=true" else tokenUrl
        val result = producerTemplate.request(endpoint) { e ->
            e.message.body = form
            e.message.headers.putAll(headers)
        }

        val status = result.message.getHeader(Exchange.HTTP_RESPONSE_CODE, Int::class.java) ?: 0
        val contentType = result.message.getHeader(Exchange.CONTENT_TYPE, String::class.java) ?: ""
        val responseBody = result.message.getBody(String::class.java)

        if (status !in 200..299) {
            logger.warn("Token endpoint responded with status {} and body: {}", status, responseBody)
            throw IllegalArgumentException("Invalid authorization code or token exchange failed (status $status)")
        }

        // Basic sanity check: ensure we got JSON back
        if (!contentType.contains("json", ignoreCase = true) && responseBody?.trim()?.startsWith("{") != true) {
            logger.warn("Token endpoint returned non-JSON response. Content-Type='{}', body snippet='{}'", contentType, responseBody?.take(80))
            throw IllegalArgumentException("Token endpoint returned unexpected response format")
        }

        val json: JsonNode = objectMapper.readTree(responseBody)
        val accessToken = json.get("access_token")?.asText()
            ?: throw IllegalArgumentException("Token response missing access_token")

        // Store token for downstream processors and set Authorization header
        exchange.setProperty("access_token", accessToken)
        exchange.message.setHeader("Authorization", "Bearer $accessToken")

        logger.info("Token exchange completed successfully")
    }

    private fun buildFormBody(code: String): String {
        val enc = StandardCharsets.UTF_8.name()
        val base = listOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "client_id" to clientId,
            "redirect_uri" to redirectUri
        ).toMutableList()
        val clientSecret = clientSecretOpt.orElse("")
        if (clientSecret.isNotBlank()) base += ("client_secret" to clientSecret)
        return base.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, enc) + "=" + URLEncoder.encode(v, enc)
        }
    }
}
