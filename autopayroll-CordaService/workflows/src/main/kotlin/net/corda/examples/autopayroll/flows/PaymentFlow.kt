package net.corda.examples.autopayroll.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.examples.autopayroll.contracts.MoneyStateContract
import net.corda.examples.autopayroll.states.MoneyState
import net.corda.examples.autopayroll.states.PaymentRequestState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.identity.CordaX500Name

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
        val output = MoneyState(vaultState.amount.toInt(), vaultState.toWhom)

        val transactionBuilder = TransactionBuilder(notary)
        val commandData = MoneyStateContract.Commands.Pay()
        transactionBuilder.addCommand(commandData, ourIdentity.owningKey, vaultState.toWhom.owningKey)
        transactionBuilder.addOutputState(output, MoneyStateContract.ID)
        transactionBuilder.verify(serviceHub)

        val session = initiateFlow(vaultState.toWhom)
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
                if (counterpartySession.counterparty != serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("BankOperator", "Toronto", "CA"))!!) {
                    throw FlowException("Only Bank Node can send a payment state")
                }
            }
        }

        val txId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))

    }
}
