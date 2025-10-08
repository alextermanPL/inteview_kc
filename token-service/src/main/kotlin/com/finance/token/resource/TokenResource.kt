package com.finance.token.resource

import com.finance.token.service.TokenFlowService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory

@Path("/token")
@ApplicationScoped
class TokenResource {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenResource::class.java)
    }

    @Inject
    lateinit var tokenFlowService: TokenFlowService

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun handle(@QueryParam("code") code: String?): Response {
        if (code.isNullOrBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "missing 'code' query parameter"))
                .build()
        }

        logger.info("Handling OAuth code: {}", code.take(8))

        return try {
            val result = tokenFlowService.handle(code)
            Response.ok(result).build()
        } catch (e: Exception) {
            logger.error("Error in token flow", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(mapOf("error" to "Failed to process token"))
                .build()
        }
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): Map<String, Any> =
        mapOf("status" to "UP", "service" to "Token Service", "timestamp" to java.time.Instant.now().toString())
}