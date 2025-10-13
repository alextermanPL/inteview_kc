package com.finance.token.it

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.common.QuarkusTestResource
import io.restassured.RestAssured.given
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

@QuarkusTest
@QuarkusTestResource(ContainersResource::class)
class TokenServiceIT {

    @Test
    fun `publishes to real kafka after wiremocked calls`() {
        // Trigger the route
        given()
            .queryParam("code", "validcode123")
        .`when`()
            .get("/token")
        .then()
            .statusCode(200)

        // Consume from Kafka
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ContainersResource.kafka.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "it-group")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        }
        KafkaConsumer<String, String>(props).use { consumer ->
            consumer.subscribe(listOf("user-transactions"))
            val records = consumer.poll(Duration.ofSeconds(10))
            val found = records.any { it.value().contains("\"user\":\"itest-user\"") }
            assertTrue(found, "Expected a message for user 'itest-user'")
        }
    }
}

