package com.autopayroll.flows

import co.paralleluniverse.fibers.Suspendable
import com.autopayroll.contracts.MoneyStateContract
import com.autopayroll.states.MoneyState
import com.autopayroll.states.PaymentRequestState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.CollectSignaturesFlow




// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByService
class PaymentFlowInitiator : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction{
        // Initiator flow logic goes here

        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val wBStateList =  serviceHub.vaultService.queryBy(PaymentRequestState::class.java).states
        val vaultState = wBStateList.get(wBStateList.size -1).state.data
        val output = MoneyState(vaultState.amount.toInt(),vaultState.towhom)


        val transactionBuilder = TransactionBuilder(notary)
        val commandData = MoneyStateContract.Commands.Pay()
        transactionBuilder.addCommand(commandData, ourIdentity.owningKey, vaultState.towhom.owningKey)
        transactionBuilder.addOutputState(output, MoneyStateContract.ID)
        transactionBuilder.verify(serviceHub)


        val session = initiateFlow(vaultState.towhom)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))
        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))

    }
}

@InitiatedBy(PaymentFlowInitiator::class)
class PaymentFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: Checking.
            }
        }

        val txId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))

    }
}
