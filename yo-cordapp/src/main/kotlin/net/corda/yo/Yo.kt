package net.corda.yo

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.getOrThrow
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// API.
@Path("yo")
class YoApi(val rpcOps: CordaRPCOps) {
    @GET
    @Path("yo")
    @Produces(MediaType.APPLICATION_JSON)
    fun yo(@QueryParam(value = "target") target: String): Response {
        val (status, message) = try {
            // Look-up the 'target'.
            val matches = rpcOps.partiesFromName(target, exactMatch = true)

            // We only want one result!
            val to: Party = when {
                matches.isEmpty() -> throw IllegalArgumentException("Target string doesn't match any nodes on the network.")
                matches.size > 1 -> throw IllegalArgumentException("Target string matches multiple nodes on the network.")
                else -> matches.single()
            }

            // Start the flow, block and wait for the response.
            val result = rpcOps.startFlowDynamic(YoFlow::class.java, to).returnValue.getOrThrow()
            // Return the response.
            Response.Status.CREATED to "You just sent a Yo! to ${to.name} (Transaction ID: ${result.tx.id})"
        } catch (e: Exception) {
            Response.Status.BAD_REQUEST to e.message
        }
        return Response.status(status).entity(message).build()
    }

    @GET
    @Path("yos")
    @Produces(MediaType.APPLICATION_JSON)
    fun yos() = rpcOps.vaultQuery(YoState::class.java).states

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun me() = mapOf("me" to rpcOps.nodeInfo().legalIdentities.first().name)

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun peers() = mapOf("peers" to rpcOps.networkMapSnapshot().map { it.legalIdentities.first().name })
}

// Flow.
@InitiatingFlow
@StartableByRPC
class YoFlow(val target: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = YoFlow.tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating a new Yo!")
        object SIGNING : ProgressTracker.Step("Verifying the Yo!")
        object VERIFYING : ProgressTracker.Step("Verifying the Yo!")
        object FINALISING : ProgressTracker.Step("Sending the Yo!") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING

        val me = serviceHub.myInfo.legalIdentities.first()
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val command = Command(YoContract.Send(), listOf(me.owningKey))
        val state = YoState(me, target)
        val stateAndContract = StateAndContract(state, YO_CONTRACT_ID)
        val utx = TransactionBuilder(notary = notary).withItems(stateAndContract, command)

        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = VERIFYING
        stx.verify(serviceHub)

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(stx, FINALISING.childProgressTracker()))
    }
}

// Contract and state.
const val YO_CONTRACT_ID = "net.corda.yo.YoContract"

class YoContract: Contract {

    // Command.
    class Send : TypeOnlyCommandData()

    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Send>()
        "There can be no inputs when Yo'ing other parties." using (tx.inputs.isEmpty())
        "There must be one output: The Yo!" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<YoState>().single()
        "No sending Yo's to yourself!" using (yo.target != yo.origin)
        "The Yo! must be signed by the sender." using (yo.origin.owningKey == command.signers.single())
    }
}

// State.
data class YoState(val origin: Party,
                 val target: Party,
                 val yo: String = "Yo!") : ContractState, QueryableState {
    override val participants get() = listOf(target)
    override fun toString() = "${origin.name}: $yo"
    override fun supportedSchemas() = listOf(YoSchemaV1)
    override fun generateMappedObject(schema: MappedSchema) = YoSchemaV1.PersistentYoState(
            origin.name.toString(), target.name.toString(), yo)

    object YoSchema

    object YoSchemaV1 : MappedSchema(YoSchema.javaClass, 1, listOf(PersistentYoState::class.java)) {
        @Entity
        @Table(name = "yos")
        class PersistentYoState(
                @Column(name = "origin")
                var origin: String = "",
                @Column(name = "target")
                var target: String = "",
                @Column(name = "yo")
                var yo: String = ""
        ) : PersistentState()
    }
}

class YoWebPlugin : WebServerPluginRegistry {
    override val webApis = listOf(Function(::YoApi))
    override val staticServeDirs = mapOf("yo" to javaClass.classLoader.getResource("yoWeb").toExternalForm())
}