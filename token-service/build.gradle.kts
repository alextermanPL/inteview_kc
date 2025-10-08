dependencies {
    // JAX-RS entrypoint, JSON
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")

    // Camel Quarkus (versions come from quarkus-camel-bom in root)
    implementation("org.apache.camel.quarkus:camel-quarkus-core")
    implementation("org.apache.camel.quarkus:camel-quarkus-direct")
    implementation("org.apache.camel.quarkus:camel-quarkus-http")
    implementation("org.apache.camel.quarkus:camel-quarkus-kafka")
    implementation("org.apache.camel.quarkus:camel-quarkus-jackson")

    // Client assertion signer
    implementation("org.bitbucket.b_c:jose4j:0.9.6")
}
