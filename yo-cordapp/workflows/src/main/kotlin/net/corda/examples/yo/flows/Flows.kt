package net.corda.examples.yo.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.examples.yo.contracts.YoContract
import net.corda.examples.yo.states.YoState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class YoFlow(val target: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = tracker()

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
        val command = Command(YoContract.Commands.Send(), listOf(me.owningKey))
        val state = YoState(me, target)
        val stateAndContract = StateAndContract(state, YoContract.ID)
        val utx = TransactionBuilder(notary = notary).withItems(stateAndContract, command)

        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = VERIFYING
        stx.verify(serviceHub)

        progressTracker.currentStep = FINALISING
        val targetSession = initiateFlow(target)
        return subFlow(FinalityFlow(stx, listOf(targetSession), FINALISING.childProgressTracker()))
    }
}

@InitiatedBy(YoFlow::class)
class YoFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}