package com.finance.token.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.component.kafka.KafkaConstants
import org.slf4j.LoggerFactory

@ApplicationScoped
class KafkaPublisherProcessor : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaPublisherProcessor::class.java)
    }

    @Inject
    lateinit var objectMapper: ObjectMapper

    override fun process(exchange: Exchange) {
        logger.info("Preparing Kafka message for publishing")

        val raw = exchange.message.getBody(String::class.java) ?: "{}"
        val node: JsonNode = try { objectMapper.readTree(raw) } catch (e: Exception) { objectMapper.createObjectNode() }

        // Determine user and transactions payload
        val (user, transactionsNode) = when {
            node.has("userId") && node.has("transactions") -> {
                node.get("userId").asText() to node.get("transactions")
            }
            node.isArray -> {
                "testuser" to node
            }
            else -> {
                "testuser" to (node.get("transactions") ?: objectMapper.createArrayNode())
            }
        }

        val envelope: ObjectNode = objectMapper.createObjectNode()
            .put("user", user)
        envelope.set<JsonNode>("transactions", transactionsNode)

        val json = objectMapper.writeValueAsString(envelope)
        exchange.message.body = json

        // Optional Kafka headers (key helps partitioning by user)
        exchange.message.setHeader(KafkaConstants.KEY, user)

        logger.info("Kafka message prepared for user={} ({} bytes)", user, json.length)
    }
}
