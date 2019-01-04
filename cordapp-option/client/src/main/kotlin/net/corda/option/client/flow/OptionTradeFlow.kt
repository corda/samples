package net.corda.option.client.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.option.base.DUMMY_CURRENT_DATE
import net.corda.option.base.ORACLE_NAME
import net.corda.option.base.contract.OptionContract
import net.corda.option.base.contract.OptionContract.Companion.OPTION_CONTRACT_ID
import net.corda.option.base.flow.QueryOracle
import net.corda.option.base.flow.RequestOracleSig
import net.corda.option.base.state.OptionState
import java.time.Duration
import java.time.Instant
import java.util.function.Predicate

object OptionTradeFlow {

    /**
     * Transfers an option to a new owner. The existing owner gets no payment in return.
     *
     * @property linearId the ID of the option to be transferred.
     * @property newOwner the owner the option is being transferred to.
     */
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val linearId: UniqueIdentifier, private val newOwner: Party) : FlowLogic<SignedTransaction>() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object RETRIEVING_THE_INPUTS : ProgressTracker.Step("We retrieve the option to exercise from the vault.")
            object SENDING_THE_EXISTING_OPTION : ProgressTracker.Step("We send the existing option to the counterparty.")

            fun tracker() = ProgressTracker(RETRIEVING_THE_INPUTS, SENDING_THE_EXISTING_OPTION)
        }

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = RETRIEVING_THE_INPUTS
            val stateAndRef = serviceHub.getStateAndRefByLinearId<OptionState>(linearId)
            val inputOption = stateAndRef.state.data

            // Option trades can only be initiated by the option's current owner.
            require(ourIdentity == inputOption.owner) { "Option trades can only be initiated by the current owner." }

            progressTracker.currentStep = SENDING_THE_EXISTING_OPTION
            val counterpartySession = initiateFlow(newOwner)
            subFlow(SendStateAndRefFlow(counterpartySession, listOf(stateAndRef)))

            val flow = object : SignTransactionFlow(counterpartySession) {
                @Suspendable
                override fun checkTransaction(stx: SignedTransaction) {
                    // We check the oracle is a required signer. If so, we can trust the spot price and volatility data.
                    val oracle = serviceHub.firstIdentityByName(ORACLE_NAME)
                    stx.requiredSigningKeys.contains(oracle.owningKey)
                }
            }

            val txId = subFlow(flow).id
            return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
        }
    }

    @InitiatingFlow
    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object SET_UP : ProgressTracker.Step("Initialising flow.")
            object RECEIVING_INPUT_OPTION : ProgressTracker.Step("We receive the input option from the counterparty.")
            object QUERYING_THE_ORACLE : ProgressTracker.Step("Querying oracle for the current spot price and volatility.")
            object BUILDING_THE_TX : ProgressTracker.Step("Building transaction.")
            object ADDING_CASH_PAYMENT : ProgressTracker.Step("Adding the cash to cover the premium.")
            object VERIFYING_THE_TX : ProgressTracker.Step("Verifying transaction.")
            object WE_SIGN : ProgressTracker.Step("signing transaction.")
            object ORACLE_SIGNS : ProgressTracker.Step("Requesting oracle signature.")
            object OTHERS_SIGN : ProgressTracker.Step("Requesting old owner's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(SET_UP, RECEIVING_INPUT_OPTION, QUERYING_THE_ORACLE, BUILDING_THE_TX,
                    ADDING_CASH_PAYMENT, VERIFYING_THE_TX, WE_SIGN, ORACLE_SIGNS, OTHERS_SIGN, FINALISING)
        }

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = SET_UP
            // In Corda v1.0, we identify oracles we want to use by name.
            val oracle = serviceHub.firstIdentityByName(ORACLE_NAME)

            progressTracker.currentStep = RECEIVING_INPUT_OPTION
            val stateAndRef = subFlow(ReceiveStateAndRefFlow<OptionState>(counterpartySession)).single()
            val inputOption = stateAndRef.state.data

            progressTracker.currentStep = QUERYING_THE_ORACLE
            val (spotPrice, volatility) = subFlow(QueryOracle(oracle, inputOption.underlyingStock, DUMMY_CURRENT_DATE))

            progressTracker.currentStep = BUILDING_THE_TX
            val outputState = inputOption.copy(owner = ourIdentity, spotPriceAtPurchase = spotPrice.value)

            val requiredSigners = listOf(inputOption.owner, ourIdentity).map { it.owningKey }
            val tradeCommand = Command(OptionContract.Commands.Trade(), requiredSigners)
            val oracleCommand = Command(OptionContract.OracleCommand(spotPrice, volatility), oracle.owningKey)

            val builder = TransactionBuilder(stateAndRef.state.notary)
                    .addInputState(stateAndRef)
                    .addOutputState(outputState, OPTION_CONTRACT_ID)
                    .addCommand(tradeCommand)
                    .addCommand(oracleCommand)
                    .setTimeWindow(Instant.now(), Duration.ofSeconds(60))

            progressTracker.currentStep = ADDING_CASH_PAYMENT
            val optionPrice = OptionState.calculatePremium(inputOption, volatility)
            Cash.generateSpend(serviceHub, builder, optionPrice, ourIdentityAndCert, inputOption.owner)

            progressTracker.currentStep = VERIFYING_THE_TX
            builder.verify(serviceHub)

            progressTracker.currentStep = WE_SIGN
            val ptx = serviceHub.signInitialTransaction(builder)

            progressTracker.currentStep = ORACLE_SIGNS
            // For privacy reasons, we only want to expose to the oracle any commands of type
            // `OptionContract.OracleCommand` that require its signature.
            val ftx = ptx.buildFilteredTransaction(Predicate {
                it is Command<*>
                        && it.value is OptionContract.OracleCommand
                        && oracle.owningKey in it.signers
            })

            val oracleSignature = subFlow(RequestOracleSig(oracle, ftx))
            val ptxWithOracleSig = ptx.withAdditionalSignature(oracleSignature)

            progressTracker.currentStep = OTHERS_SIGN
            val stx = subFlow(CollectSignaturesFlow(ptxWithOracleSig, listOf(counterpartySession), OTHERS_SIGN.childProgressTracker()))

            progressTracker.currentStep = FINALISING
            var finalitySessions = listOf(counterpartySession)
            if (ourIdentity != inputOption.issuer) {
                finalitySessions = finalitySessions.plus(initiateFlow(inputOption.issuer))
            }
            return subFlow(FinalityFlow(stx, finalitySessions))
        }
    }

    @InitiatedBy(Responder::class)
    class FinalityResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            subFlow(ReceiveFinalityFlow(counterpartySession))
        }
    }
}
