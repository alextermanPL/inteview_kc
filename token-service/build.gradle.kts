apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

dependencies {
    // Align Camel Quarkus artifacts with a BOM matching Quarkus 3.5.x
    implementation(platform("org.apache.camel.quarkus:camel-quarkus-bom:3.5.0"))
    // Camel Quarkus for REST exposure via Quarkus HTTP
    implementation("org.apache.camel.quarkus:camel-quarkus-rest")
    implementation("org.apache.camel.quarkus:camel-quarkus-platform-http")

    // HTTP client for Keycloak and transactions-service calls
    implementation("org.apache.camel.quarkus:camel-quarkus-http")

    // Kafka producer
    implementation("org.apache.camel.quarkus:camel-quarkus-kafka")

    // Direct component (used for internal subroutes)
    implementation("org.apache.camel.quarkus:camel-quarkus-direct")

    // JSON marshalling
    implementation("org.apache.camel.quarkus:camel-quarkus-jackson")

    // Micrometer metrics + Prometheus
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("org.apache.camel.quarkus:camel-quarkus-micrometer")

    // JWT client assertion signing
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")

    // Test libs
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.apache.camel:camel-core:3.21.0")
    testImplementation("org.apache.camel.quarkus:camel-quarkus-mock")
    testImplementation("org.testcontainers:kafka:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.1")
    testImplementation("org.apache.kafka:kafka-clients:3.6.0")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
}

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks {
    // Task to copy keys from root project to resources
    val copyKeys by registering(Copy::class) {
        from("${rootProject.projectDir}/keys")
        into("${project.projectDir}/src/main/resources/keys")
        include("*.pem", "*.jks", "*.p12", "*.json")
    }

    // Run copyKeys before processResources
    named("processResources") {
        dependsOn(copyKeys)
    }
    
    // Also run before tests
    named("processTestResources") {
        dependsOn(copyKeys)
    }
}
