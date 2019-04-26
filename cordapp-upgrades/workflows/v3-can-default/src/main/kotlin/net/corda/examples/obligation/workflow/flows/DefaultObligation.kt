package net.corda.examples.obligation.workflow.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.obligation.contract.Obligation
import net.corda.examples.obligation.contract.ObligationContract


// V3: Add a new flow to update the the new field in the contract state. Note that as this flow did not exist in
// previous versions of the workflows jar, it does not need to handle old versions of FinalityFlow.
object DefaultObligation {

    @StartableByRPC
    @InitiatingFlow(version = 1)
    class Initiator(private val linearId: UniqueIdentifier) : ObligationBaseFlow() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object GET_OBLIGATION : ProgressTracker.Step("Obtaining obligation from vault.")
            object CHECK_INITIATOR : ProgressTracker.Step("Checking current borrower is initiating flow.")
            object BUILD_TRANSACTION : ProgressTracker.Step("Building and verifying transaction.")
            object SIGN_TRANSACTION : ProgressTracker.Step("Signing transaction.")
            object SYNC_OUR_IDENTITY : ProgressTracker.Step("Syncing our identity with the counterparty.") {
                override fun childProgressTracker() = IdentitySyncFlow.Send.tracker()
            }

            object COLLECT_SIGS : ProgressTracker.Step("Collecting counterparty signatures.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISE : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(GET_OBLIGATION, CHECK_INITIATOR, BUILD_TRANSACTION, SIGN_TRANSACTION, SYNC_OUR_IDENTITY, COLLECT_SIGS, FINALISE)
        }

        @Suspendable
        override fun call(): SignedTransaction {
            // Stage 1. Retrieve obligation with the correct linear ID from the vault.
            progressTracker.currentStep = GET_OBLIGATION
            val obligationToDefault = getObligationByLinearId(linearId)
            val inputObligation = obligationToDefault.state.data

            val lender = getLenderIdentity(inputObligation)
            val lenderFlowSession = initiateFlow(lender)
            val sessions = setOf(lenderFlowSession)

            // Stage 2. This flow can only be initiated by the current borrower. Abort if the lender started this flow.
            progressTracker.currentStep = CHECK_INITIATOR
            flowCheck(ourIdentity == getBorrowerIdentity(inputObligation)) { "Obligation default can only be initiated by the borrower." }

            // Stage 3. Create the new obligation state.
            progressTracker.currentStep = BUILD_TRANSACTION
            val defaultedObligation = createOutputObligation(inputObligation)

            // Stage 4. Create the default command.
            val signers = inputObligation.participants
            val signerKeys = signers.map { it.owningKey }
            val defaultCommand = Command(ObligationContract.Commands.Default(), signerKeys)

            // Stage 5. Create a transaction builder, add the states and commands, and verify the output.
            val builder = TransactionBuilder(firstNotary)
                    .addInputState(obligationToDefault)
                    .addOutputState(defaultedObligation, ObligationContract.OBLIGATION_CONTRACT_ID)
                    .addCommand(defaultCommand)
            builder.verify(serviceHub)

            // Stage 6. Sign the transaction using the key we originally used.
            progressTracker.currentStep = SIGN_TRANSACTION
            val ptx = serviceHub.signInitialTransaction(builder, inputObligation.borrower.owningKey)

            // Stage 7. Share our anonymous identity with the lender
            progressTracker.currentStep = SYNC_OUR_IDENTITY
            subFlow(IdentitySyncFlow.Send(sessions, ptx.tx, Companion.SYNC_OUR_IDENTITY.childProgressTracker()))

            // Stage 8. Collect signatures from the lender
            progressTracker.currentStep = COLLECT_SIGS
            val stx = subFlow(CollectSignaturesFlow(
                    ptx, sessions, listOf(inputObligation.borrower.owningKey), Companion.COLLECT_SIGS.childProgressTracker()))

            // Stage 9. Notarise and record the transaction in our vaults.
            progressTracker.currentStep = FINALISE
            return subFlow(FinalityFlow(stx, sessions, FINALISE.childProgressTracker()))
        }

        @Suspendable
        private fun getLenderIdentity(inputObligation: Obligation): Party {
            return if (inputObligation.lender is AnonymousParty) {
                resolveIdentity(inputObligation.lender)
            } else {
                inputObligation.lender as Party
            }
        }

        @Suspendable
        private fun createOutputObligation(inputObligation: Obligation): Obligation {
            return inputObligation.withDefaulted()
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

    @InitiatedBy(DefaultObligation.Initiator::class)
    class Responder(private val otherFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object SYNC_IDENTITY : ProgressTracker.Step("Syncing our identity with the current borrower.")
            object SIGN_TRANSACTION : ProgressTracker.Step("Signing transaction.") {
                override fun childProgressTracker() = IdentitySyncFlow.Send.tracker()
            }

            fun tracker() = ProgressTracker(SYNC_IDENTITY, SIGN_TRANSACTION)
        }

        @Suspendable
        override fun call(): SignedTransaction {
            // Stage 1. Sync identities with the current lender.
            progressTracker.currentStep = SYNC_IDENTITY
            subFlow(IdentitySyncFlow.Receive(otherFlow))

            // Stage 2. Sign the transaction.
            progressTracker.currentStep = SIGN_TRANSACTION
            val stx = subFlow(SignTxFlowNoChecking(otherFlow))

            return subFlow(ReceiveFinalityFlow(otherFlow, stx.id))
        }
    }
}