package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.InvoiceContract
import com.example.flow.IssueInvoiceFlow.Acceptor
import com.example.flow.IssueInvoiceFlow.Initiator
import com.example.service.RateOf
import com.example.state.InvoiceState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import java.time.LocalDate
import java.util.function.Predicate

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the Invoice encapsulated
 * within an [InvoiceState].
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * Sample call:
 *   start IssueInvoiceFlow hoursWorked: 8, date: 2019-05-20, otherParty: "O=MegaCorp 1,L=New York,C=US"
 */
object IssueInvoiceFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val hoursWorked: Int,
                    private val date: LocalDate,
                    private val otherParty: Party) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object DETERMINING_SALARY : Step("Determining salary rate for contractor/company.")
            object GENERATING_TRANSACTION : Step("Generating transaction based on new hours submission.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object ORACLE_SIGNS : Step("Requesting oracle signature.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    DETERMINING_SALARY,
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    ORACLE_SIGNS,
                    GATHERING_SIGS,
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

            // Stage 1.
            progressTracker.currentStep = DETERMINING_SALARY
            // Query the SalaryRateOracle for the billable rate
            val oracleName = CordaX500Name("Oracle", "London","GB")
            val oracle = serviceHub.networkMapCache.getNodeByLegalName(oracleName)?.legalIdentities?.first()
                    ?: throw IllegalArgumentException("Requested oracle $oracleName not found on network.")

            val rate = subFlow(QueryRate(RateOf(serviceHub.myInfo.legalIdentities[0], otherParty), oracle))

            // Stage 2.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val contractor = serviceHub.myInfo.legalIdentities.first()
            val invoiceState = InvoiceState(date, hoursWorked, rate.value, contractor, otherParty, oracle)
            val commandSigners = invoiceState.participants.plus(oracle).map { it.owningKey }
            val txCommand = Command(InvoiceContract.Commands.Create(contractor, otherParty, rate.value), commandSigners)
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(invoiceState, InvoiceContract.ID)
                    .addCommand(txCommand)

            // Stage 3.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 4.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 5.
            progressTracker.currentStep = ORACLE_SIGNS
            // Have the oracle sign the transaction
            val ftx = partSignedTx.buildFilteredTransaction(Predicate {
                when (it) {
                    is Command<*> -> oracle.owningKey in it.signers && it.value is InvoiceContract.Commands.Create
                    else -> false
                }
            })
            partSignedTx.withAdditionalSignature(subFlow(SignRate(txBuilder, oracle, ftx)))

            // Stage 6.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartySession = initiateFlow(otherParty)
            val oracleSession = initiateFlow(oracle)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession, oracleSession), GATHERING_SIGS.childProgressTracker()))

            // Stage 7.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in all parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession, oracleSession), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatingFlow
    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        lateinit var invoice : InvoiceState
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an invoice transaction." using (output is InvoiceState)
                    invoice = output as InvoiceState
                    "Invoices with a value over 10 aren't accepted." using (invoice.hoursWorked <= 10)
                }
            }
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}
