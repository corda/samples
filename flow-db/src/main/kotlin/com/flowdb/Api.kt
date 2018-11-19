package com.flowdb

import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.webserver.services.WebServerPluginRegistry
import org.slf4j.Logger
import java.util.function.Function
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

@Path("token")
class TokenApi(private val rpcOps: CordaRPCOps) {
    companion object {
        private val logger: Logger = loggerFor<TokenApi>()
    }

    @PUT
    @Path("add-token")
    fun addToken(@QueryParam("token") token: String, @QueryParam("value") value: Int): Response {
        return try {
            val flowHandle = rpcOps.startFlow(::AddTokenValueFlow, token, value)
            flowHandle.returnValue.getOrThrow()
            Response.status(CREATED).entity("Token $token added to the table.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @POST
    @Path("update-token")
    fun updateToken(@QueryParam("token") token: String, @QueryParam("value") value: Int): Response {
        return try {
            val flowHandle = rpcOps.startFlow(::UpdateTokenValueFlow, token, value)
            flowHandle.returnValue.getOrThrow()
            Response.status(CREATED).entity("Token $token updated in the table.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @GET
    @Path("query-token")
    fun queryToken(@QueryParam("token") token: String): Response {
        return try {
            val flowHandle = rpcOps.startFlow(::QueryTokenValueFlow, token)
            val value = flowHandle.returnValue.getOrThrow()
            Response.status(CREATED).entity("Token $token is valued at $value.\n").build()
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
}

class TokenPlugin : WebServerPluginRegistry {
    override val webApis = listOf(Function(::TokenApi))
}