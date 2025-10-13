package com.finance.token.processor

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import org.apache.camel.Exchange
import org.apache.camel.Processor

@ApplicationScoped
@Alternative
@Priority(1)
class TestTokenExchangeProcessor : Processor {
    override fun process(exchange: Exchange) {
        // Simulate successful token exchange
        exchange.setProperty("access_token", "test-token")
        exchange.message.setHeader("Authorization", "Bearer test-token")
    }
}

