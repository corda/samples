package net.corda.demos.crowdFunding.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import net.corda.demos.crowdFunding.contracts.CampaignContract
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import java.time.Instant
import java.util.*

/**
 * A flow that handles the pledging of money to a specific crowd funding campaign. A pledge is always initiated
 * by the pledger. We can do it this way because the campaign manager broadcasts the campaign state to all parties
 * on the crowd funding business network.
 */
@StartableByRPC
@InitiatingFlow
class MakePledge(
        private val amount: Amount<Currency>,
        private val campaignReference: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Pick a notary. Don't care which one.
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        // Get the Campaign state corresponding to the provided ID from our vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(campaignReference))
        val campaignInputStateAndRef = serviceHub.vaultService.queryBy<Campaign>(queryCriteria).states.single()
        val campaignState = campaignInputStateAndRef.state.data

        // Assemble the other transaction components.
        // Commands:
        // We need a Create Pledge command and a Campaign Pledge command, as well as the Campaign input + output and
        // the new Pledge output state. The new pledge needs to be signed by the campaign manager and the pledger as
        // it is a bi-lateral agreement. The pledge to campaign command only needs to be signed by the campaign
        // manager. Either way, both the manager and the pledger need to sign this transaction.
        val createPledgeCommand = Command(CampaignContract.Commands.Pledge(), listOf(ourIdentity.owningKey))

        // Output states:
        // We create a new pledge state that reflects the requested amount, referencing the campaign Id we want to
        // pledge money to. We then need to update the amount of money this campaign has raised and add the linear
        // id of the new pledge to the campaign.
        val pledgeOutputState = Pledge(amount, ourIdentity, campaignState.manager, campaignReference)
        val pledgeOutputStateAndContract = StateAndContract(pledgeOutputState, CampaignContract.CONTRACT_REF)
        val newRaisedSoFar = campaignState.raisedSoFar + amount
        val campaignOutputState = campaignState.copy(raisedSoFar = newRaisedSoFar)
        val campaignOutputStateAndContract = StateAndContract(campaignOutputState, CampaignContract.CONTRACT_REF)

        // Build the transaction.
        val utx = TransactionBuilder(notary = notary).withItems(
                pledgeOutputStateAndContract, // Output
                campaignOutputStateAndContract, // Output
                campaignInputStateAndRef, // Input
                createPledgeCommand  // Command
        )

        // Set the time for when this transaction happened.
        utx.setTimeWindow(Instant.now(), 30.seconds)

        // Sign the transaction.
        val stx = serviceHub.signInitialTransaction(utx)

        // Get a list of all identities from the network map cache.
        val everyoneButMeAndNotaries = serviceHub.networkMapCache.allNodes
                .flatMap { it.legalIdentities }
                .filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity
        val sessions = everyoneButMeAndNotaries.map { initiateFlow(it) }

        // Finalise this transaction and broadcast to all parties.
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(MakePledge::class)
class RecordPledgeAsObserver(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Receive and record the new campaign state in our vault EVEN THOUGH we are not a participant as we are
        // using 'ALL_VISIBLE'.
        subFlow(ReceiveFinalityFlow(counterpartySession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}
