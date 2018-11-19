package com.whistleblower

import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

/**
 * A REST API for interacting with the BlowWhistle CorDapp.
 *
 * The endpoints are all defined as GET for ease-of-use from the browser.
 *
 * @param rpcOps a connection to the node via RPC.
 * @property me the node's identity.
 */
@Path("a")
class BlowWhistleAPI(private val rpcOps: CordaRPCOps) {
    private val me = rpcOps.nodeInfo().legalIdentities.first()

    /** Returns the open whistle-blowing cases. */
    @GET
    @Path("cases")
    @Produces(MediaType.APPLICATION_JSON)
    fun openCases() = rpcOps.vaultQueryBy<BlowWhistleState>().states.map { it.state.data }

    /**
     * Blows the whistle on [badCompanyName] to [investigatorName].
     */
    @GET
    @Path("blow-whistle")
    fun blowWhistle(
            @QueryParam("company") badCompanyName: String,
            @QueryParam("to") investigatorName: String): Response {

        val badCompany = rpcOps.partiesFromName(badCompanyName, false).singleOrNull()
        val investigator = rpcOps.partiesFromName(investigatorName, false).singleOrNull()
        if (badCompany == null || investigator == null) {
            val errMsg = "Party could not be retrieved from network map using name provided."
            return Response.status(BAD_REQUEST).entity(errMsg).build()
        }

        return try {
            rpcOps.startFlow(::BlowWhistleFlow, badCompany, investigator).returnValue.getOrThrow()
            val successMsg = "$me reported ${badCompany.name.organisation} to ${investigator.name.organisation}."
            Response.status(CREATED).entity(successMsg).build()

        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }
}

class WebPlugin : WebServerPluginRegistry {
    override val webApis = listOf(Function(::BlowWhistleAPI))
}