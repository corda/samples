package net.corda.examples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.obligation.Obligation
import net.corda.examples.obligation.ObligationContract
import net.corda.examples.obligation.ObligationContract.Companion.OBLIGATION_CONTRACT_ID

object TransferObligation {

    @StartableByRPC
    @InitiatingFlow
    class Initiator(private val linearId: UniqueIdentifier,
                    private val newLender: Party,
                    private val anonymous: Boolean = true) : ObligationBaseFlow() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object PREPARATION : ProgressTracker.Step("Obtaining Obligation from vault.")
            object BUILDING : ProgressTracker.Step("Building and verifying transaction.")
            object SIGNING : ProgressTracker.Step("signing transaction.")
            object SYNCING : ProgressTracker.Step("Syncing identities.") {
                override fun childProgressTracker() = IdentitySyncFlow.Send.tracker()
            }
            object COLLECTING : ProgressTracker.Step("Collecting counterparty signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(PREPARATION, BUILDING, SIGNING, SYNCING, COLLECTING, FINALISING)
        }

        @Suspendable
        override fun call(): SignedTransaction {
            // Stage 1. Retrieve obligation specified by linearId from the vault.
            progressTracker.currentStep = PREPARATION
            val obligationToTransfer = getObligationByLinearId(linearId)
            val inputObligation = obligationToTransfer.state.data

            // Stage 2. This flow can only be initiated by the current recipient.
            val lenderIdentity = getLenderIdentity(inputObligation)

            // Stage 3. Abort if the borrower started this flow.
            check(ourIdentity == lenderIdentity) { "Obligation transfer can only be initiated by the lender." }

            // Stage 4. Create the new obligation state reflecting a new lender.
            progressTracker.currentStep = BUILDING
            val transferredObligation = createOutputObligation(inputObligation)

            // Stage 4. Create the transfer command.
            val signers = inputObligation.participants + transferredObligation.lender
            val signerKeys = signers.map { it.owningKey }
            val transferCommand = Command(ObligationContract.Commands.Transfer(), signerKeys)

            // Stage 5. Create a transaction builder, then add the states and commands.
            val builder = TransactionBuilder(firstNotary)
                    .addInputState(obligationToTransfer)
                    .addOutputState(transferredObligation, OBLIGATION_CONTRACT_ID)
                    .addCommand(transferCommand)

            // Stage 6. Verify and sign the transaction.
            progressTracker.currentStep = SIGNING
            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder, inputObligation.lender.owningKey)

            // Stage 7. Get a Party object for the borrower.
            progressTracker.currentStep = SYNCING
            val borrower = getBorrowerIdentity(inputObligation)

            // Stage 8. Send any keys and certificates so the signers can verify each other's identity.
            // We call `toSet` in case the borrower and the new lender are the same party.
            val sessions = listOf(borrower, newLender).toSet().map { party: Party -> initiateFlow(party) }.toSet()
            subFlow(IdentitySyncFlow.Send(sessions, ptx.tx, SYNCING.childProgressTracker()))

            // Stage 9. Collect signatures from the borrower and the new lender.
            progressTracker.currentStep = COLLECTING
            val stx = subFlow(CollectSignaturesFlow(
                    partiallySignedTx = ptx,
                    sessionsToCollectFrom = sessions,
                    myOptionalKeys = listOf(inputObligation.lender.owningKey),
                    progressTracker = COLLECTING.childProgressTracker())
            )

            // Stage 10. Notarise and record, the transaction in our vaults. Send a copy to me as well.
            progressTracker.currentStep = FINALISING
            return subFlow(FinalityFlow(stx, setOf(ourIdentity)))
        }

        @Suspendable
        private fun getLenderIdentity(inputObligation: Obligation): AbstractParty {
            return if (inputObligation.lender is AnonymousParty) {
                resolveIdentity(inputObligation.lender)
            } else {
                inputObligation.lender
            }
        }

        @Suspendable
        private fun createOutputObligation(inputObligation: Obligation): Obligation {
            return if (anonymous) {
                // TODO: Is there a flow to get a key and cert only from the counterparty?
                val txKeys = subFlow(SwapIdentitiesFlow(newLender))
                val anonymousLender = txKeys[newLender] ?: throw FlowException("Couldn't get lender's conf. identity.")
                inputObligation.withNewLender(anonymousLender)
            } else {
                inputObligation.withNewLender(newLender)
            }
        }

        @Suspendable
        private fun getBorrowerIdentity(inputObligation: Obligation): Party {
            return if (inputObligation.borrower is AnonymousParty) {
                resolveIdentity(inputObligation.borrower)
            } else {
                inputObligation.borrower as Party
            }
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val otherFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            subFlow(IdentitySyncFlow.Receive(otherFlow))
            val stx = subFlow(SignTxFlowNoChecking(otherFlow))
            return waitForLedgerCommit(stx.id)
        }
    }
}