package com.finance.token.processor

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.ProducerTemplate
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.net.ConnectException

@ApplicationScoped
class TransactionFetcherProcessor : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(TransactionFetcherProcessor::class.java)
    }

    @Inject
    lateinit var producerTemplate: ProducerTemplate

    @ConfigProperty(name = "transactions.api.url")
    lateinit var transactionsUrl: String

    override fun process(exchange: Exchange) {
        logger.info("Fetching transactions from transactions-service")

        val auth = exchange.message.getHeader("Authorization", String::class.java)
            ?: throw IllegalArgumentException("Missing Authorization header for transactions-service call")

        val headers = mutableMapOf<String, Any>(
            Exchange.HTTP_METHOD to "GET",
            "Authorization" to auth
        )

        val endpoint = if (transactionsUrl.startsWith("http")) "$transactionsUrl?throwExceptionOnFailure=true" else transactionsUrl
        val result = producerTemplate.request(endpoint) { e ->
            e.message.headers.putAll(headers)
        }

        val status = result.message.getHeader(Exchange.HTTP_RESPONSE_CODE, Int::class.java) ?: 200
        val body = result.message.getBody(String::class.java)

        if (status !in 200..299) {
            logger.warn("transactions-service responded with status {} and body: {}", status, body)
            throw ConnectException("transactions-service unavailable or unauthorized")
        }

        // Pass the JSON response body downstream
        exchange.message.body = body
        logger.info("Transactions fetched successfully")
    }
}
