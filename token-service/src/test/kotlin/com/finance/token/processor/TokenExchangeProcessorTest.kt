package com.finance.token.processor

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TokenExchangeProcessorTest {

    private lateinit var ctx: CamelContext
    private lateinit var template: ProducerTemplate
    private lateinit var processor: TokenExchangeProcessor

    @BeforeEach
    fun setup() {
        ctx = DefaultCamelContext()
        template = mock()
        processor = TokenExchangeProcessor().apply {
            producerTemplate = template
            objectMapper = ObjectMapper()
            tokenUrl = "http://keycloak/token"
            clientId = "finance-client"
            redirectUri = "http://localhost:8081/token"
            clientSecretOpt = java.util.Optional.empty()
        }
    }

    @Test
    fun `exchanges code and sets authorization header`() {
        val exchangeIn = DefaultExchange(ctx)
        exchangeIn.message.setHeader("code", "authcode123")

        val httpExchange = DefaultExchange(ctx)
        httpExchange.message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200)
        httpExchange.message.body = "{" + "\"access_token\":\"abc\"" + "}"

        whenever(template.request(any<String>(), any())).thenReturn(httpExchange)

        processor.process(exchangeIn)

        val token = exchangeIn.getProperty("access_token", String::class.java)
        val auth = exchangeIn.message.getHeader("Authorization", String::class.java)
        assertEquals("abc", token)
        assertEquals("Bearer abc", auth)
    }

    @Test
    fun `throws on missing code`() {
        val exchangeIn = DefaultExchange(ctx)
        assertThrows(IllegalArgumentException::class.java) { processor.process(exchangeIn) }
    }
}
