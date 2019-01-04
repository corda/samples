package net.corda.option.client.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.option.base.contract.OptionContract
import net.corda.option.base.contract.OptionContract.Companion.OPTION_CONTRACT_ID
import net.corda.option.base.state.OptionState
import java.time.Duration
import java.time.Instant

object OptionExerciseFlow {

    /**
     * Exercises the option to lock in the exercise date on the ledger. At a later date, the issuer, owner and oracle
     * can come together to redeem the option for its cash value.
     *
     * @property linearId the ID of the option to be exercised.
     */
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
        companion object {
            object SET_UP : ProgressTracker.Step("Initialising flow.")
            object RETRIEVING_THE_INPUTS : ProgressTracker.Step("We retrieve the option to exercise from the vault.")
            object BUILDING_THE_TX : ProgressTracker.Step("Building transaction.")
            object VERIFYING_THE_TX : ProgressTracker.Step("Verifying transaction.")
            object WE_SIGN : ProgressTracker.Step("signing transaction.")
            object FINALISING : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(SET_UP, BUILDING_THE_TX, RETRIEVING_THE_INPUTS, VERIFYING_THE_TX, WE_SIGN,
                    FINALISING)
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = SET_UP
            val notary = serviceHub.firstNotary()

            progressTracker.currentStep = RETRIEVING_THE_INPUTS
            val stateAndRef = serviceHub.getStateAndRefByLinearId<OptionState>(linearId)
            val inputOption = stateAndRef.state.data

            // This flow can only be called by the option's current owner.
            require(inputOption.owner == ourIdentity) { "Option exercise flow must be initiated by the current owner."}

            progressTracker.currentStep = BUILDING_THE_TX
            val exercisedOption = inputOption.copy(exercised = true, exercisedOnDate = Instant.now())
            val exerciseCommand = Command(OptionContract.Commands.Exercise(), inputOption.owner.owningKey)

            // Add the state and the command to the builder.
            val builder = TransactionBuilder(notary)
                    .addInputState(stateAndRef)
                    .addOutputState(exercisedOption, OPTION_CONTRACT_ID)
                    .addCommand(exerciseCommand)
                    .setTimeWindow(Instant.now(), Duration.ofSeconds(60))

            progressTracker.currentStep = VERIFYING_THE_TX
            builder.verify(serviceHub)

            progressTracker.currentStep = WE_SIGN
            val stx = serviceHub.signInitialTransaction(builder)

            val sessions = (inputOption.participants - ourIdentity).map { initiateFlow(it) }.toSet()

            progressTracker.currentStep = FINALISING
            return subFlow(FinalityFlow(stx, sessions))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            subFlow(ReceiveFinalityFlow(counterpartySession))
        }
    }
}
