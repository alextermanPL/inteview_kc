package com.finance.token.security

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.File
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*

@ApplicationScoped
class ClientJwtService {

    @ConfigProperty(name = "keycloak.client.id")
    lateinit var clientId: String

    @ConfigProperty(name = "keycloak.token.url")
    lateinit var tokenUrl: String

    @ConfigProperty(name = "keycloak.issuer.url")
    lateinit var issuerUrlOpt: java.util.Optional<String>

    @ConfigProperty(name = "keycloak.client.jwt.private-key", defaultValue = "keys/private_key.pem")
    lateinit var privateKeyPath: String

    @ConfigProperty(name = "keycloak.client.jwt.kid")
    lateinit var keyIdOpt: java.util.Optional<String>

    private lateinit var privateKey: PrivateKey

    @PostConstruct
    fun init() {
        privateKey = loadPrivateKeyPem(privateKeyPath)
    }

    fun buildClientAssertion(): String {
        val now = Instant.now()
        val aud = issuerUrlOpt.orElse("").ifBlank { tokenUrl }
        val claims = JWTClaimsSet.Builder()
            .issuer(clientId)
            .subject(clientId)
            .audience(aud)
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(60)))
            .build()

        val headerBuilder = JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
        val kid = keyIdOpt.orElse("")
        if (kid.isNotBlank()) headerBuilder.keyID(kid)
        val header = headerBuilder.build()

        val signed = SignedJWT(header, claims)
        signed.sign(RSASSASigner(privateKey))
        return signed.serialize()
    }

    private fun loadPrivateKeyPem(path: String): PrivateKey {
        // Try as-is (relative to current working dir)
        val primary = File(path)
        val content: String? = when {
            primary.exists() -> Files.readString(primary.toPath())
            else -> {
                // Try one directory up (common when running from submodule)
                val parent = File("..", path)
                when {
                    parent.exists() -> Files.readString(parent.toPath())
                    else -> ClientJwtService::class.java.classLoader.getResource(path)?.readText()
                }
            }
        }
        require(!content.isNullOrBlank()) { "Private key not found at $path (also tried ../$path and classpath)" }

        val base64 = content
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val decoded = Base64.getDecoder().decode(base64)
        val spec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }
}
