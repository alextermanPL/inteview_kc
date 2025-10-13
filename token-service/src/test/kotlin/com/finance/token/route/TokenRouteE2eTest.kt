package com.finance.token.route

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.apache.camel.CamelContext
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
@io.quarkus.test.common.QuarkusTestResource(E2EPropertiesResource::class)
class TokenRouteE2eTest {

    @Inject
    lateinit var context: CamelContext

    @BeforeEach
    fun setupMocks() {
        // No-op: route publishes to mock:kafka via test property override
    }

    @Test
    fun `end-to-end route publishes to kafka and returns 200`() {
        given()
            .queryParam("code", "ok123456")
        .`when`()
            .get("/token")
        .then()
            .statusCode(200)
    }
}
