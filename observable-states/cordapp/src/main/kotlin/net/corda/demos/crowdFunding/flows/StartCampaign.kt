package net.corda.demos.crowdFunding.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.demos.crowdFunding.contracts.CampaignContract
import net.corda.demos.crowdFunding.structures.Campaign

/**
 * A flow that handles the starting of a new campaigns. It creates a new [Campaign] and stores it in in the vault of the
 * node that runs this flow (the manager of the campaign) and then broadcasts it to all the other nodes on the
 * crowdFunding business network.
 *
 * The nodes receiving the broadcast use the observable states feature by recording all visible output states despite
 * the fact the only participant for the [Campaign] start is the [manager] of the [Campaign].
 */
@StartableByRPC
class StartCampaign(private val newCampaign: Campaign) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): SignedTransaction {
        // Pick a notary. Don't care which one.
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        // Assemble the transaction components.
        val startCommand = Command(CampaignContract.Start(), listOf(ourIdentity.owningKey))
        val outputState = StateAndContract(newCampaign, CampaignContract.CONTRACT_REF)

        // Build, sign and record the transaction.
        val utx = TransactionBuilder(notary = notary).withItems(outputState, startCommand)
        val stx = serviceHub.signInitialTransaction(utx)
        val ftx = subFlow(FinalityFlow(stx))

        // Broadcast this transaction to all parties on this business network.
        subFlow(BroadcastTransaction(ftx))

        return ftx
    }

}