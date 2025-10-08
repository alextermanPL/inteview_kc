package com.finance.token.processors

import com.finance.token.config.JwtAssertionSigner
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.component.http.HttpMethods
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ApplicationScoped
class ExchangeCodeForToken : Processor {

    @Inject lateinit var signer: JwtAssertionSigner

    @ConfigProperty(name = "kc.token.url") lateinit var kcTokenUrl: String
    @ConfigProperty(name = "oidc.client.id") lateinit var clientId: String
    @ConfigProperty(name = "oidc.redirect.uri") lateinit var redirectUri: String
    @ConfigProperty(name = "oidc.client.assertion.kid") lateinit var clientKid: String
    @ConfigProperty(name = "oidc.client.assertion.audience") lateinit var clientAudience: String
    @ConfigProperty(name = "oidc.client.private-key.path") lateinit var privateKeyPath: String

    override fun process(exchange: Exchange) {
        val code = exchange.`in`.getHeader("code", String::class.java)
            ?: throw IllegalArgumentException("missing code")

        val assertion = signer.signClientAssertion(
            clientId = clientId,
            audience = clientAudience,
            kid = clientKid,
            privateKeyPath = privateKeyPath
        )

        val form = buildString {
            append("grant_type=authorization_code")
            append("&code=").append(url(code))
            append("&redirect_uri=").append(url(redirectUri))
            append("&client_id=").append(url(clientId))
            append("&client_assertion_type=").append(url("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"))
            append("&client_assertion=").append(url(assertion))
        }

        exchange.setProperty("kcTokenUrl", kcTokenUrl)
        exchange.`in`.apply {
            setHeader(Exchange.HTTP_METHOD, HttpMethods.POST)
            setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded")
            body = form
        }
    }

    private fun url(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)
}