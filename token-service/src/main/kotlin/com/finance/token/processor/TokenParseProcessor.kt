package com.finance.token.processor

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor

@ApplicationScoped
class TokenParseProcessor : Processor {

    @Inject
    lateinit var objectMapper: ObjectMapper

    override fun process(exchange: Exchange) {
        val status = exchange.message.getHeader(Exchange.HTTP_RESPONSE_CODE, Int::class.java) ?: 0
        val contentType = exchange.message.getHeader(Exchange.CONTENT_TYPE, String::class.java) ?: ""
        val body = exchange.message.getBody(String::class.java)

        if (status !in 200..299) {
            throw IllegalArgumentException("Invalid authorization code or token exchange failed (status $status)")
        }
        if (!contentType.contains("json", true) && body?.trim()?.startsWith("{") != true) {
            throw IllegalArgumentException("Token endpoint returned unexpected response format")
        }

        val node = objectMapper.readTree(body)
        val accessToken = node.get("access_token")?.asText()
            ?: throw IllegalArgumentException("Token response missing access_token")
        exchange.setProperty("access_token", accessToken)
        exchange.message.setHeader("Authorization", "Bearer $accessToken")
    }
}

