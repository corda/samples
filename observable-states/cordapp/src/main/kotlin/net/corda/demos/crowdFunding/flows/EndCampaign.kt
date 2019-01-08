package net.corda.demos.crowdFunding.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import net.corda.demos.crowdFunding.contracts.CampaignContract
import net.corda.demos.crowdFunding.pledgersForCampaign
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.CampaignResult
import net.corda.demos.crowdFunding.structures.CashStatesPayload
import net.corda.finance.contracts.asset.Cash

/**
 * A flow that deals with ending the campaign, whether it successfully reaches its target or not. If the
 * campaign reaches the target then the manager sends a [CampaignResult.Success] object to all the pledgers, asking them
 * to provide cash states equal to the pledge they previously made. They send back the cash states to the manager who
 * assembles the transaction which exits the campaign and pledge states, and transfers the cash to the campaign manager.
 */
@SchedulableFlow
@InitiatingFlow
class EndCampaign(private val stateRef: StateRef) : FlowLogic<SignedTransaction>() {

    /**
     * Sends a Success message to each one of the pledgers. In response, they send back cash states equal to the
     * amount which they previously pledged. We also need to receive the stateRefs from them, so we can verify the
     * transaction that we end up building.
     * */
    @Suspendable
    fun requestPledgedCash(sessions: List<FlowSession>): CashStatesPayload {
        // Send a request to each pledger and get the dependency transactions as well.
        val cashStates = sessions.map { session ->
            // Send "Success" message.
            session.send(CampaignResult.Success(stateRef))
            // Resolve transactions for the given StateRefs.
            subFlow(ReceiveStateAndRefFlow<ContractState>(session))
            // Receive the cash inputs, outputs and public keys.
            session.receive<CashStatesPayload>().unwrap { it }
        }

        // Return all of the collected states and keys.
        return CashStatesPayload(
                cashStates.flatMap { it.inputs },
                cashStates.flatMap { it.outputs },
                cashStates.flatMap { it.signingKeys }
        )
    }

    /** Common stuff that happens whether we meet the target of not. */
    private fun cancelPledges(campaign: Campaign): TransactionBuilder {
        // Pick a notary. Don't care which one.
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val utx = TransactionBuilder(notary = notary)

        // Create inputs.
        val pledgerStateAndRefs = pledgersForCampaign(serviceHub, campaign)
        val campaignInputStateAndRef = serviceHub.toStateAndRef<Campaign>(stateRef)

        // Create commands.
        val endCampaignCommand = Command(CampaignContract.Commands.End(), campaign.manager.owningKey)

        // Add the above
        pledgerStateAndRefs.forEach { utx.addInputState(it) }
        utx.addInputState(campaignInputStateAndRef)
        utx.addCommand(endCampaignCommand)

        return utx
    }

    /** Do the common stuff then request the pledged cash. Once we have all the states, put them in the builder. */
    @Suspendable
    fun handleSuccess(campaign: Campaign, sessions: List<FlowSession>): TransactionBuilder {
        // Do the stuff we must do anyway.
        val utx = cancelPledges(campaign)

        // Gather the cash states from the pledgers.
        val cashStates = requestPledgedCash(sessions)

        // Add the cash inputs, outputs and command.
        cashStates.inputs.forEach { utx.addInputState(it) }
        cashStates.outputs.forEach { utx.addOutputState(it, Cash.PROGRAM_ID) }
        utx.addCommand(Cash.Commands.Move(), cashStates.signingKeys)

        return utx
    }

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the actual state from the ref.
        val campaign = serviceHub.loadState(stateRef).data as Campaign

        // As all nodes have the campaign state, all will try to start this flow. Abort for all but the manger.
        if (campaign.manager != ourIdentity) {
            throw FlowException("Only the campaign manager can run this flow.")
        }

        // Get the pledges for this campaign. Remember, everyone has a copy of them.
        val pledgersForCampaign = pledgersForCampaign(serviceHub, campaign)

        // Create flow sessions for all pledgers.
        val sessions = pledgersForCampaign.map { (state) -> initiateFlow(state.data.pledger) }

        // Do different things depending on whether we've raised enough, or not.
        val utx = when {
            campaign.raisedSoFar < campaign.target -> {
                sessions.forEach { session -> session.send(CampaignResult.Failure()) }
                cancelPledges(campaign)
            }
            else -> handleSuccess(campaign, sessions)
        }

        // Sign, finalise and distribute the transaction.
        val ptx = serviceHub.signInitialTransaction(utx)
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions.map { it }))

        // Finalise this transaction and broadcast to all parties.
        return subFlow(BroadcastCampaignEnd(stx))
    }
}

@InitiatingFlow
class BroadcastCampaignEnd(val stx: SignedTransaction) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Get a list of all identities from the network map cache.
        val everyoneButMeAndNotaries = serviceHub.networkMapCache.allNodes
                .flatMap { it.legalIdentities }
                .filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity
        val sessions = everyoneButMeAndNotaries.map { initiateFlow(it) }

        // Finalise this transaction and broadcast to all parties.
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(EndCampaign::class)
class SettlePledge(val counterpartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val campaignResult = counterpartySession.receive<CampaignResult>().unwrap { it }

        if (campaignResult is CampaignResult.Success) {
            handleSuccess(campaignResult.campaignRef)
            val flow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) = Unit // TODO
            }

            subFlow(flow)
        }
    }

    @Suspendable
    fun handleSuccess(campaignRef: StateRef) {
        // Get our Pledge state for this campaign.
        val campaign = serviceHub.loadState(campaignRef).data as Campaign
        val results = pledgersForCampaign(serviceHub, campaign)

        // Find our pledge. We have to do this as we have ALL the pledges for this campaign in our vault.
        // This is because ReceiveTransactionFlow only allows us to record the WHOLE SignedTransaction and not a
        // filtered transaction. In an ideal World, we would be able to send a filtered transaction that only shows
        // the Campaign state and not the pledge states, so we would ONLY ever have our Pledge states in the vault.
        // From a privacy perspective, this doesn't matter, though. As all the pledgers generate random keys.
        val amount = results.single { (state) ->
            state.data.pledger == ourIdentity
        }.state.data.amount

        // Using generate spend is the best way to get cash states to spend.
        val (utx, _) = Cash.generateSpend(serviceHub, TransactionBuilder(), amount, campaign.manager)

        // The Cash contract design won't allow more than one move command per transaction. As, we are collecting
        // cash from potentially multiple parties, we have pull out the items from the transaction builder so we
        // can add them back to the OTHER transaction builder but with only ONE move command.
        val inputStateAndRefs = utx.inputStates().map { serviceHub.toStateAndRef<Cash.State>(it) }
        val outputStates = utx.outputStates().map { it.data as Cash.State }
        val signingKeys = utx.commands().flatMap { it.signers }

        // We need to send the cash state dependency transactions so the manager can verify the tx proposal.
        subFlow(SendStateAndRefFlow(counterpartySession, inputStateAndRefs))

        // Send the payload back to the campaign manager.
        val pledgedCashStates = CashStatesPayload(inputStateAndRefs, outputStates, signingKeys)
        counterpartySession.send(pledgedCashStates)
    }
}

@InitiatedBy(BroadcastCampaignEnd::class)
class RecordCampaignEndAsObserver(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Receive and record the new campaign state in our vault EVEN THOUGH we are not a participant as we are
        // using 'ALL_VISIBLE'.
        subFlow(ReceiveFinalityFlow(counterpartySession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}