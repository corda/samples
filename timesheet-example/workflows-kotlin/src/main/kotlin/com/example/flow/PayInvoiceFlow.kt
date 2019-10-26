package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.InvoiceContract
import com.example.flow.PayInvoiceFlow.Acceptor
import com.example.flow.PayInvoiceFlow.Initiator
import com.example.state.InvoiceState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.finance.POUNDS
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.workflows.asset.CashUtils
import java.util.*

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [InvoiceState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object PayInvoiceFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val invoiceId: UUID) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new hours submission.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {

            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val invoiceAndRef = serviceHub.vaultService.queryBy<InvoiceState>(QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier(id = invoiceId)))).states.single()
            val invoice = invoiceAndRef.state.data

            val paymentAmount = (invoice.hoursWorked * invoice.rate).POUNDS
            // We're MegaCorp.  Let's print some money.
            subFlow(CashIssueFlow(paymentAmount, OpaqueBytes.of(1), notary))

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val txCommand = Command(InvoiceContract.Commands.Pay(), serviceHub.myInfo.legalIdentities[0].owningKey)
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(invoiceAndRef)
                    .addOutputState(invoice.copy(paid = true), InvoiceContract.ID)
                    .addCommand(txCommand)
            // Add our payment to the contractor
            CashUtils.generateSpend(serviceHub, txBuilder, paymentAmount, serviceHub.myInfo.legalIdentitiesAndCerts[0], invoice.contractor)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            val contractorSession = initiateFlow(invoice.contractor)
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in all parties' vaults.
            return subFlow(FinalityFlow(signedTx, contractorSession))
        }
    }

    @InitiatingFlow
    @InitiatedBy(Initiator::class)
    class Acceptor(private val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(otherPartySession))
        }
    }
}
