package com.sendfile.flows

import co.paralleluniverse.fibers.Suspendable
import com.sendfile.contracts.InvoiceContract
import com.sendfile.states.InvoiceState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.io.File

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SendAttachment(
        private val receiver: Party
) : FlowLogic<SignedTransaction>() {
    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction")
        object PROCESS_TRANSACTION : ProgressTracker.Step("PROCESS transaction")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                PROCESS_TRANSACTION,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()
    @Suspendable
    override fun call():SignedTransaction {
        //initiate notary
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        //Initiate transaction builder
        val transactionBuilder = TransactionBuilder(notary)

        //upload attachment via private method
        val attachmenthash = SecureHash.parse(uploadAttachment("../../../test.zip",
                serviceHub,
                ourIdentity,
                "testzip"))

        //build transaction
        val ouput = InvoiceState(attachmenthash.toString(),participants = listOf(ourIdentity,receiver))
        val commandData = InvoiceContract.Commands.Issue()
        transactionBuilder.addCommand(commandData,ourIdentity.owningKey,receiver.owningKey)
        transactionBuilder.addOutputState(ouput,InvoiceContract.ID)
        transactionBuilder.addAttachment(attachmenthash)
        transactionBuilder.verify(serviceHub)

        //self signing
        progressTracker.currentStep = PROCESS_TRANSACTION
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)


        //conter parties signing
        progressTracker.currentStep = FINALISING_TRANSACTION

        val session = initiateFlow(receiver)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))
    }
}


//private helper method
private fun uploadAttachment(
        path: String,
        service: ServiceHub,
        whoAmI: Party,
        filename: String
): String {
    val attachmenthash = service.attachments.importAttachment(
            File(path).inputStream(),
            whoAmI.toString(),
            filename)

    return attachmenthash.toString();
}


@InitiatedBy(SendAttachment::class)
class SendAttachmentResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: Checking.
                if (stx.tx.attachments.isEmpty()) {
                    throw FlowException("No Jar was being sent")
                }

            }
        }
        val txId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}