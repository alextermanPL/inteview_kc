package com.finance.token.processor

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
import java.net.ConnectException

class TransactionFetcherProcessorTest {

    private lateinit var ctx: CamelContext
    private lateinit var template: ProducerTemplate
    private lateinit var processor: TransactionFetcherProcessor

    @BeforeEach
    fun setup() {
        ctx = DefaultCamelContext()
        template = mock()
        processor = TransactionFetcherProcessor().apply {
            producerTemplate = template
            transactionsUrl = "http://localhost:8082/api/transactions"
        }
    }

    @Test
    fun `fetches transactions successfully`() {
        val exchangeIn = DefaultExchange(ctx)
        exchangeIn.message.setHeader("Authorization", "Bearer token123")

        val httpExchange = DefaultExchange(ctx)
        httpExchange.message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200)
        httpExchange.message.body = "{\"userId\":\"testuser\",\"transactions\":[{}]}"

        whenever(template.request(any<String>(), any())).thenReturn(httpExchange)

        processor.process(exchangeIn)

        assertEquals("{\"userId\":\"testuser\",\"transactions\":[{}]}", exchangeIn.message.getBody(String::class.java))
    }

    @Test
    fun `throws on non-2xx status`() {
        val exchangeIn = DefaultExchange(ctx)
        exchangeIn.message.setHeader("Authorization", "Bearer token123")

        val httpExchange = DefaultExchange(ctx)
        httpExchange.message.setHeader(Exchange.HTTP_RESPONSE_CODE, 401)
        httpExchange.message.body = "{}"

        whenever(template.request(any<String>(), any())).thenReturn(httpExchange)

        assertThrows(ConnectException::class.java) { processor.process(exchangeIn) }
    }
}

