package net.corda.examples.attachments.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.examples.attachments.contract.AgreementContract
import net.corda.examples.attachments.contract.AgreementContract.Companion.AGREEMENT_CONTRACT_ID
import net.corda.examples.attachments.state.AgreementState

@InitiatingFlow
@StartableByRPC
class ProposeFlow(private val agreementTxt: String,
                  private val untrustedPartiesAttachment: SecureHash,
                  private val counterparty: Party) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val agreementState = AgreementState(ourIdentity, counterparty, agreementTxt)
        val agreeCmd = AgreementContract.Commands.Agree()
        val agreeCmdRequiredSigners = listOf(ourIdentity.owningKey, counterparty.owningKey)

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(agreementState, AGREEMENT_CONTRACT_ID)
                .addCommand(agreeCmd, agreeCmdRequiredSigners)
                .addAttachment(untrustedPartiesAttachment)

        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        val counterpartySession = initiateFlow(counterparty)
        val signedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterpartySession)))

        return subFlow(FinalityFlow(signedTx, listOf(counterpartySession)))
    }
}

@InitiatedBy(ProposeFlow::class)
class AgreeFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // We ensure that the transaction contains an AgreementContract.
                if (stx.toLedgerTransaction(serviceHub, false).outputsOfType<AgreementState>().isEmpty()) {
                    throw FlowException("Agreement transaction did not contain an output AgreementState.")
                }
                // We delegate checking entirely to the AgreementContract.
            }
        }

        val txId = subFlow(signTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, txId))
    }
}