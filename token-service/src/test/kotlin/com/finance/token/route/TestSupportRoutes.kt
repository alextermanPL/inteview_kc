package com.finance.token.route

import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

@ApplicationScoped
class TestSupportRoutes : RouteBuilder() {
    override fun configure() {
        // Simulate Keycloak token endpoint
        from("direct:keycloak")
            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(200)
            .setBody().constant("{\"access_token\":\"e2e-token\"}")

        // Simulate transactions-service
        from("direct:tx")
            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(200)
            .setBody().constant("{\"userId\":\"e2e-user\",\"transactions\":[{\"id\":\"1\"}]}")
    }
}
