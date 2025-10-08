package com.finance.token.config

import jakarta.enterprise.context.ApplicationScoped
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.NumericDate
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.UUID

@ApplicationScoped
class JwtAssertionSigner {

    fun signClientAssertion(
        clientId: String,
        audience: String,
        kid: String?,
        privateKeyPath: String
    ): String {
        val now = Instant.now()

        val claims = JwtClaims().apply {
            issuer = clientId
            subject = clientId
            setAudience(audience) // <- correct setter
            jwtId = UUID.randomUUID().toString()
            issuedAt = NumericDate.fromSeconds(now.epochSecond)
            expirationTime = NumericDate.fromSeconds(now.plusSeconds(60).epochSecond)
        }

        val privateKey = readPkcs8RsaPrivateKey(privateKeyPath)

        val jws = JsonWebSignature().apply {
            payload = claims.toJson()
            key = privateKey
            algorithmHeaderValue = AlgorithmIdentifiers.RSA_USING_SHA256
            setHeader("typ", "JWT")
            if (!kid.isNullOrBlank()) setKeyIdHeaderValue(kid)
        }
        return jws.compactSerialization
    }

    private fun readPkcs8RsaPrivateKey(path: String): PrivateKey {
        val pem = String(Files.readAllBytes(Path.of(path)))
        val base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(base64)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }
}
