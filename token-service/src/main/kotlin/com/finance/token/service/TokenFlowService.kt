package com.finance.token.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.ProducerTemplate
import org.slf4j.LoggerFactory

@ApplicationScoped
class TokenFlowService {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenFlowService::class.java)
    }

    @Inject
    lateinit var template: ProducerTemplate

    fun handle(code: String): Map<String, Any> {
        val headers = mapOf("code" to code)
        val body = template.requestBodyAndHeaders("direct:tokenFlow", null, headers, Any::class.java)

        @Suppress("UNCHECKED_CAST")
        return when (body) {
            is Map<*, *> -> body as Map<String, Any>
            is String -> mapOf("status" to "published", "raw" to body)
            else -> mapOf("status" to "published")
        }
    }
}