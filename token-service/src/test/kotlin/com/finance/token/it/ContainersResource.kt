package com.finance.token.it

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ContainersResource : QuarkusTestResourceLifecycleManager {
    companion object {
        lateinit var kafka: KafkaContainer
        lateinit var wiremock: GenericContainer<*>
    }

    override fun start(): MutableMap<String, String> {
        kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.1"))
        kafka.start()

        wiremock = GenericContainer(DockerImageName.parse("wiremock/wiremock:2.35.0")).withExposedPorts(8080)
        wiremock.start()

        val wmHost = "http://localhost:${wiremock.getMappedPort(8080)}"
        // Configure stubs via admin API
        postMapping(wmHost, """
            {
              "request": {"method": "POST", "url": "/realms/finance-app/protocol/openid-connect/token"},
              "response": {"status": 200, "headers": {"Content-Type":"application/json"}, "body": "{\"access_token\":\"itoken\"}"}
            }
        """.trimIndent())

        postMapping(wmHost, """
            {
              "request": {"method": "GET", "url": "/api/transactions", "headers": {"Authorization": {"matches": "Bearer .*"}}},
              "response": {"status": 200, "headers": {"Content-Type":"application/json"}, "body": "{\"userId\":\"itest-user\",\"transactions\":[{\"id\":\"1\"}]}"}
            }
        """.trimIndent())

        return mutableMapOf(
            "camel.component.kafka.brokers" to kafka.bootstrapServers,
            "kafka.publish.endpoint" to "kafka:user-transactions",
            "keycloak.token.url" to "$wmHost/realms/finance-app/protocol/openid-connect/token",
            "transactions.api.url" to "$wmHost/api/transactions",
            "quarkus.kafka.devservices.enabled" to "false"
        )
    }

    private fun postMapping(host: String, body: String) {
        val client = HttpClient.newHttpClient()
        val req = HttpRequest.newBuilder()
            .uri(URI.create("$host/__admin/mappings"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        client.send(req, HttpResponse.BodyHandlers.ofString())
    }

    override fun stop() {
        runCatching { wiremock.stop() }
        runCatching { kafka.stop() }
    }
}
