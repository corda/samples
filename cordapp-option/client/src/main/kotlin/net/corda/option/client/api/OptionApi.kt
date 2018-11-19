package net.corda.option.client.api

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.getCashBalances
import net.corda.option.base.OptionType
import net.corda.option.base.state.OptionState
import net.corda.option.client.flow.OptionExerciseFlow
import net.corda.option.client.flow.OptionIssueFlow
import net.corda.option.client.flow.OptionTradeFlow
import net.corda.option.client.flow.SelfIssueCashFlow
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NODE_NAME = CordaX500Name("Notary", "London", "GB")

/**
 * This API is accessible from /api/option. The endpoint paths specified below are relative to it.
 * We've defined a bunch of endpoints to deal with options and cash and the various operations you can perform with them.
 */
@Path("option")
class OptionApi(val rpcOps: CordaRPCOps) {
    private val me = rpcOps.nodeInfo().legalIdentities.first()
    private val myLegalName = me.name

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                .filter { it !in listOf(myLegalName, SERVICE_NODE_NAME) })
    }

    /**
     * Returns all stocks the oracle can offer prices for.
     */
    @GET
    @Path("stocks")
    @Produces(MediaType.APPLICATION_JSON)
    fun getStocks() = mapOf("stocks" to listOf(
            "The Carlsbad National Bank",
            "Wilburton State Bank",
            "De Soto State Bank",
            "Florida Traditions Bank",
            "CorEast Federal Savings Bank")
    )

    /**
     * Displays all option states that exist in the node's vault.
     */
    @GET
    @Path("options")
    @Produces(MediaType.APPLICATION_JSON)
    fun getOptions() = rpcOps.vaultQueryBy<OptionState>().states.filter { it.state.data.owner == me }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCash() = rpcOps.getCashBalances()

    /**
     * Issues a new option onto the ledger.
     */
    @GET
    @Path("issue-option")
    fun issueOption(@QueryParam(value = "strike") strike: Int,
                    @QueryParam(value = "currency") currency: String,
                    @QueryParam(value = "expiry") expiry: String,
                    @QueryParam(value = "underlying") underlying: String,
                    @QueryParam(value = "issuer") issuerName: CordaX500Name,
                    @QueryParam(value = "optionType") optionType: String): Response {

        // We construct the option to be issued.
        val issuer = rpcOps.wellKnownPartyFromX500Name(issuerName) ?: throw IllegalArgumentException("Unknown issuer.")
        val expiryDate = LocalDate.parse(expiry).atStartOfDay().toInstant(ZoneOffset.UTC)
        val type = if (optionType == "CALL") OptionType.CALL else OptionType.PUT
        val strikePrice = Amount(strike.toLong() * 100, Currency.getInstance(currency))
        val optionToIssue = OptionState(
                strikePrice = strikePrice,
                expiryDate = expiryDate,
                underlyingStock = underlying,
                issuer = issuer,
                owner = me,
                optionType = type)

        // We issue the option onto the ledger.
        return try {
            val flowHandle = rpcOps.startFlow(OptionIssueFlow::Initiator, optionToIssue)
            val flowResult = flowHandle.returnValue.getOrThrow()
            // Return the response.
            Response.status(CREATED).entity("Option issued onto the ledger: ${flowResult.tx.outputsOfType<OptionState>().single()}.").build()
        } catch (e: Exception) {
            // For the purposes of this demo app, we do not differentiate by exception type.
            Response.status(BAD_REQUEST).entity(e.message).build()
        }
    }

    /**
     * Transfers an option identified by linearId to a new party.
     */
    @GET
    @Path("trade-option")
    fun tradeOption(@QueryParam(value = "id") linearIdStr: String,
                    @QueryParam(value = "newOwner") newOwnerName: CordaX500Name): Response {
        val linearId = UniqueIdentifier.fromString(linearIdStr)
        val newOwner = rpcOps.wellKnownPartyFromX500Name(newOwnerName) ?: throw IllegalArgumentException("Unknown new owner.")

        return try {
            val flowHandle = rpcOps.startFlow(OptionTradeFlow::Initiator, linearId, newOwner)
            val flowResult = flowHandle.returnValue.getOrThrow()
            Response.status(CREATED).entity("Option transferred to new owner: ${flowResult.tx.outputsOfType<OptionState>().single()}.").build()
        } catch (e: Exception) {
            Response.status(BAD_REQUEST).entity(e.message).build()
        }
    }

    /**
     * Exercises an option. Exercised options can be redeemed for cash with the issuer.
     */
    @GET
    @Path("exercise-option")
    fun exerciseOption(@QueryParam(value = "id") linearIdStr: String): Response {
        val linearId = UniqueIdentifier.fromString(linearIdStr)

        return try {
            val flowHandle = rpcOps.startFlow(OptionExerciseFlow::Initiator, linearId)
            val flowResult = flowHandle.use { flowHandle.returnValue.getOrThrow() }
            Response.status(CREATED).entity("Option exercised: ${flowResult.tx.outputsOfType<OptionState>().single()}.").build()
        } catch (e: Exception) {
            Response.status(BAD_REQUEST).entity(e.message).build()
        }
    }

    /**
     * Helper end-point to issue ourselves some cash.
     */
    @GET
    @Path("self-issue-cash")
    fun selfIssueCash(@QueryParam(value = "amount") amount: Int,
                      @QueryParam(value = "currency") currency: String): Response {
        val issueAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))

        return try {
            val flowHandle = rpcOps.startFlow(::SelfIssueCashFlow, issueAmount)
            val cashState = flowHandle.returnValue.getOrThrow()
            Response.status(CREATED).entity(cashState.toString()).build()
        } catch (e: Exception) {
            Response.status(BAD_REQUEST).entity(e.message).build()
        }
    }
}