package com.finance.token.processor

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.finance.token.security.ClientJwtService

@ApplicationScoped
class TokenFormProcessor : Processor {

    @ConfigProperty(name = "keycloak.client.id")
    lateinit var clientId: String

    @ConfigProperty(name = "keycloak.redirect.uri", defaultValue = "http://localhost:8081/token")
    lateinit var redirectUri: String

    @ConfigProperty(name = "keycloak.client.secret")
    lateinit var clientSecretOpt: java.util.Optional<String>

    @Inject
    lateinit var clientJwtService: ClientJwtService

    override fun process(exchange: Exchange) {
        val code = exchange.message.getHeader("code", String::class.java)
            ?: throw IllegalArgumentException("Missing 'code' query parameter")

        val enc = StandardCharsets.UTF_8.name()
        val base = mutableListOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "client_id" to clientId,
            "redirect_uri" to redirectUri
        )
        val clientSecret = clientSecretOpt.orElse("")
        if (clientSecret.isNotBlank()) {
            base += ("client_secret" to clientSecret)
        } else {
            // Use private_key_jwt assertion for client-jwt auth type
            val assertion = clientJwtService.buildClientAssertion()
            base += ("client_assertion_type" to "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            base += ("client_assertion" to assertion)
        }

        val form = base.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, enc) + "=" + URLEncoder.encode(v, enc)
        }

        exchange.message.body = form
        exchange.message.setHeader(Exchange.HTTP_METHOD, "POST")
        exchange.message.setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded")
        exchange.message.setHeader("Accept", "application/json")
    }
}
