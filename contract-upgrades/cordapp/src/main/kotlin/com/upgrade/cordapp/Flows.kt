package com.upgrade.cordapp

import co.paralleluniverse.fibers.Suspendable
import com.upgrade.old.OldContract
import com.upgrade.old.OldState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class Initiator(val counterparty: Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val state = OldState(ourIdentity, counterparty)

        val myInputKeys = state.participants.map { it.owningKey }
        val txCommand = Command(OldContract.Action(), myInputKeys)

        val txBuilder = TransactionBuilder(notary).withItems(StateAndContract(state, OldContract.id), txCommand)

        txBuilder.verify(serviceHub)

        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        val otherPartyFlow = initiateFlow(counterparty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), myInputKeys))

        subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow)))
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // Check the transaction here.
            }
        }

        val stxId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = stxId))
    }
}