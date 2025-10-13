package com.finance.token.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.CamelContext
import org.apache.camel.component.kafka.KafkaConstants
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KafkaPublisherProcessorTest {

    private lateinit var ctx: CamelContext
    private lateinit var processor: KafkaPublisherProcessor
    private val mapper = ObjectMapper()

    @BeforeEach
    fun setup() {
        ctx = DefaultCamelContext()
        processor = KafkaPublisherProcessor().apply { objectMapper = mapper }
    }

    @Test
    fun `wraps transactions with user from response`() {
        val ex = DefaultExchange(ctx)
        ex.message.body = "{\"userId\":\"testuser\",\"transactions\":[{\"id\":1}]}"

        processor.process(ex)

        val json: JsonNode = mapper.readTree(ex.message.getBody(String::class.java))
        assertEquals("testuser", json.get("user").asText())
        assertEquals(1, json.get("transactions").size())
        assertEquals("testuser", ex.message.getHeader(KafkaConstants.KEY))
    }

    @Test
    fun `wraps array payload with default user`() {
        val ex = DefaultExchange(ctx)
        ex.message.body = "[{\"id\":1},{\"id\":2}]"

        processor.process(ex)

        val json: JsonNode = mapper.readTree(ex.message.getBody(String::class.java))
        assertEquals("testuser", json.get("user").asText())
        assertEquals(2, json.get("transactions").size())
    }
}

