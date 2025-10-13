package com.finance.token.route

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class E2EPropertiesResource : QuarkusTestResourceLifecycleManager {
    override fun start(): MutableMap<String, String> = mutableMapOf(
        "kafka.publish.endpoint" to "mock:kafka",
        "quarkus.kafka.devservices.enabled" to "false",
        "keycloak.token.url" to "direct:keycloak",
        "transactions.api.url" to "direct:tx"
    )
    override fun stop() {}
}

