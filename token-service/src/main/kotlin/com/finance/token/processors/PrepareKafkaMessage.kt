package com.finance.token.processors


import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.util.json.JsonObject
import org.apache.camel.util.json.Jsoner

@ApplicationScoped
class PrepareKafkaMessage : Processor {
    override fun process(exchange: Exchange) {
        val userId = exchange.getProperty("userId", String::class.java) ?: "unknown"
        val transactions = exchange.getProperty("transactionsJson", String::class.java) ?: """{"transactions":[]}"""

        val payload = JsonObject().apply {
            put("userId", userId)
            put("data", Jsoner.deserialize(transactions))
        }
        exchange.`in`.setHeader("Content-Type", "application/json")
        exchange.`in`.body = payload.toJson()
    }
}