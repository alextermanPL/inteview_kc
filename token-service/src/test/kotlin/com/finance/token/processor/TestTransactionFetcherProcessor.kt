package com.finance.token.processor

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import org.apache.camel.Exchange
import org.apache.camel.Processor

@ApplicationScoped
@Alternative
@Priority(1)
class TestTransactionFetcherProcessor : Processor {
    override fun process(exchange: Exchange) {
        // Simulate transactions-service response
        exchange.message.body = "{\"userId\":\"e2e-user\",\"transactions\":[{\"id\":\"1\"}]}"
    }
}

