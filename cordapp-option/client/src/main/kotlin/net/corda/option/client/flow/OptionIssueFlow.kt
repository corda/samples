package net.corda.option.client.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.flows.*
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

object OptionIssueFlow {

    /**
     * Purchases an option from the issuer, using the oracle to determine the correct price given the current spot
     * price and volatility.
     *
     * @property optionState the option to be purchased. Its [OptionState.spotPriceAtPurchase] will be updated as part
     *   of the flow.
     */
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val optionState: OptionState) : FlowLogic<SignedTransaction>() {

        companion object {
            object SET_UP : ProgressTracker.Step("Initialising flow.")
            object QUERYING_THE_ORACLE : ProgressTracker.Step("Querying oracle for the current spot price and volatility.")
            object BUILDING_THE_TX : ProgressTracker.Step("Building transaction.")
            object ADDING_CASH_PAYMENT : ProgressTracker.Step("Adding the cash to cover the premium.")
            object VERIFYING_THE_TX : ProgressTracker.Step("Verifying transaction.")
            object WE_SIGN : ProgressTracker.Step("Signing transaction.")
            object ORACLE_SIGNS : ProgressTracker.Step("Requesting oracle signature.")
            object OTHERS_SIGN : ProgressTracker.Step("Collecting counterparty signatures.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING : ProgressTracker.Step("Finalising transaction") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(SET_UP, QUERYING_THE_ORACLE, BUILDING_THE_TX, ADDING_CASH_PAYMENT,
                    VERIFYING_THE_TX, WE_SIGN, ORACLE_SIGNS, OTHERS_SIGN, FINALISING)
        }

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            require(optionState.owner == ourIdentity) { "Option issue flow must be initiated by the buyer." }

            progressTracker.currentStep = SET_UP
            val notary = serviceHub.firstNotary()
            // In Corda v1.0, we identify oracles we want to use by name.
            val oracle = serviceHub.firstIdentityByName(ORACLE_NAME)

            progressTracker.currentStep = QUERYING_THE_ORACLE
            val (spotPrice, volatility) = subFlow(QueryOracle(oracle, optionState.underlyingStock, DUMMY_CURRENT_DATE))
            // Store purchase spot price in state for future reference.
            optionState.spotPriceAtPurchase = spotPrice.value

            progressTracker.currentStep = BUILDING_THE_TX
            val requiredSigners = listOf(optionState.owner, optionState.issuer).map { it.owningKey }
            val issueCommand = Command(OptionContract.Commands.Issue(), requiredSigners)
            val oracleCommand = Command(OptionContract.OracleCommand(spotPrice, volatility), oracle.owningKey)

            val builder = TransactionBuilder(notary)
                    .addOutputState(optionState, OPTION_CONTRACT_ID)
                    .addCommand(issueCommand)
                    .addCommand(oracleCommand)
                    .setTimeWindow(Instant.now(), Duration.ofSeconds(60))

            progressTracker.currentStep = ADDING_CASH_PAYMENT
            val optionPrice = OptionState.calculatePremium(optionState, volatility)
            Cash.generateSpend(serviceHub, builder, optionPrice, ourIdentityAndCert, optionState.issuer)

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
            val issuerSession = initiateFlow(optionState.issuer)
            val sessions = listOf(issuerSession)

            val stx = subFlow(CollectSignaturesFlow(ptxWithOracleSig, sessions, OTHERS_SIGN.childProgressTracker()))

            progressTracker.currentStep = FINALISING
            return subFlow(FinalityFlow(stx, sessions, FINALISING.childProgressTracker()))
        }
    }

    @InitiatingFlow
    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
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
}
