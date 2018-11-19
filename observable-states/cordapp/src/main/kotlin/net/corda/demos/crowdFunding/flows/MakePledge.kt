package net.corda.demos.crowdFunding.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import net.corda.core.utilities.unwrap
import net.corda.demos.crowdFunding.contracts.CampaignContract
import net.corda.demos.crowdFunding.contracts.PledgeContract
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import java.time.Instant
import java.util.*

/**
 * This pair of flows handles the pledging of money to a specific crowd funding campaign. A pledge is always initiated
 * by the pledger. We can do it this way because the campaign manager broadcasts the campaign state to all parties
 * on the crowd funding business network.
 */
object MakePledge {

    /**
     * Takes an amount of currency and a campaign reference then creates a new pledge state and updates the existing
     * campaign state to reflect the new pledge.
     */
    @StartableByRPC
    @InitiatingFlow
    class Initiator(
            private val amount: Amount<Currency>,
            private val campaignReference: UniqueIdentifier,
            private val broadcastToObservers: Boolean
    ) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            // Pick a notary. Don't care which one.
            val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

            // Get the Campaign state corresponding to the provided ID from our vault.
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(campaignReference))
            val campaignInputStateAndRef = serviceHub.vaultService.queryBy<Campaign>(queryCriteria).states.single()
            val campaignState = campaignInputStateAndRef.state.data

            // Generate a new key and cert, so the other pledgers don't know who we are.
            val me = serviceHub.keyManagementService.freshKeyAndCert(
                    ourIdentityAndCert,
                    revocationEnabled = false
            ).party.anonymise()

            // Assemble the other transaction components.
            // Commands:
            // We need a Create Pledge command and a Campaign Pledge command, as well as the Campaign input + output and
            // the new Pledge output state. The new pledge needs to be signed by the campaign manager and the pledger as
            // it is a bi-lateral agreement. The pledge to campaign command only needs to be signed by the campaign
            // manager. Either way, both the manager and the pledger need to sign this transaction.
            val acceptPledgeCommand = Command(CampaignContract.AcceptPledge(), campaignState.manager.owningKey)
            val createPledgeCommand = Command(PledgeContract.Create(), listOf(me.owningKey, campaignState.manager.owningKey))

            // Output states:
            // We create a new pledge state that reflects the requested amount, referencing the campaign Id we want to
            // pledge money to. We then need to update the amount of money this campaign has raised and add the linear
            // id of the new pledge to the campaign.
            val pledgeOutputState = Pledge(amount, me, campaignState.manager, campaignReference)
            val pledgeOutputStateAndContract = StateAndContract(pledgeOutputState, PledgeContract.CONTRACT_REF)
            val newRaisedSoFar = campaignState.raisedSoFar + amount
            val campaignOutputState = campaignState.copy(raisedSoFar = newRaisedSoFar)
            val campaignOutputStateAndContract = StateAndContract(campaignOutputState, CampaignContract.CONTRACT_REF)

            // Build the transaction.
            val utx = TransactionBuilder(notary = notary).withItems(
                    pledgeOutputStateAndContract, // Output
                    campaignOutputStateAndContract, // Output
                    campaignInputStateAndRef, // Input
                    acceptPledgeCommand, // Command
                    createPledgeCommand  // Command
            )

            // Set the time for when this transaction happened.
            utx.setTimeWindow(Instant.now(), 30.seconds)

            // Sign, sync identities, finalise and record the transaction.
            val ptx = serviceHub.signInitialTransaction(builder = utx, signingPubKeys = listOf(me.owningKey))
            val session = initiateFlow(campaignState.manager)
            subFlow(IdentitySyncFlow.Send(otherSide = session, tx = ptx.tx))
            val stx = subFlow(CollectSignaturesFlow(ptx, setOf(session), listOf(me.owningKey)))
            val ftx = subFlow(FinalityFlow(stx))

            // Let the campaign manager know whether we want to broadcast this update to observers, or not.
            session.sendAndReceive<Unit>(broadcastToObservers)

            return ftx
        }

    }

    /**
     * This side is only run by the campaign manager who checks the proposed pledge then waits for the pledge
     * transaction to be committed and broadcasts it to all the parties on the business network.
     */
    @InitiatedBy(Initiator::class)
    class Responder(val otherSession: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(IdentitySyncFlow.Receive(otherSideSession = otherSession))

            // As the manager, we might want to do some checking of the pledge before we sign it.
            val flow = object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) = Unit // TODO: Add some checks here.
            }

            val stx = subFlow(flow)

            // Once the transaction has been committed then we then broadcast from the manager so we don't compromise
            // the confidentiality of the pledging identities, if they choose to be anonymous.
            // Only do this if the pledger asks it to be done, though.
            val broadcastToObservers = otherSession.receive<Boolean>().unwrap { it }
            if (broadcastToObservers) {
                val ftx = waitForLedgerCommit(stx.id)
                subFlow(BroadcastTransaction(ftx))
            }

            // We want the other side to block or at least wait a while for the transaction to be broadcast.
            otherSession.send(Unit)
        }

    }

}

