package net.corda.option.base.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.utils.sumCashBy
import net.corda.option.base.SpotPrice
import net.corda.option.base.Volatility
import net.corda.option.base.state.OptionState
import java.time.Duration

open class OptionContract : Contract {
    companion object {
        @JvmStatic
        val OPTION_CONTRACT_ID = "net.corda.option.base.contract.OptionContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // We should only ever receive one command at a time, else throw an exception
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> {
                requireThat {
                    // Constraints on the "shape" of the transaction.

                    // If cash states have to be combined/split into change, we may not have exactly one cash
                    // input/output.
                    val cashInputs = tx.inputsOfType<Cash.State>()
                    val cashOutputs = tx.outputsOfType<Cash.State>()
                    "Cash.State inputs are consumed" using (cashInputs.isNotEmpty())
                    "Cash.State outputs are created" using (cashOutputs.isNotEmpty())
                    "An OptionState output is created" using (tx.outputsOfType<OptionState>().size == 1)
                    "No other states are consumed" using (tx.inputs.size == cashInputs.size)
                    "No other states are created" using (tx.outputs.size == (cashOutputs.size + 1))
                    "Option issuances must be timestamped" using (tx.timeWindow != null)
                    tx.commands.requireSingleCommand<Cash.Commands.Move>()
                    val oracleCmd = tx.commands.requireSingleCommand<OptionContract.OracleCommand>()

                    // Constraints on the contents of the transaction's components.
                    val option = tx.outputsOfType<OptionState>().single()
                    val cashTransferredToIssuer = tx.outputsOfType<Cash.State>().sumCashBy(option.issuer)
                    val timeWindow = tx.timeWindow!!
                    val premium = OptionState.calculatePremium(option, oracleCmd.value.volatility)

                    "The strike price must be non-negative" using (option.strikePrice.quantity > 0)
                    "The expiry date is not in the past" using (timeWindow.untilTime!! < option.expiryDate)
                    "The option is not exercised" using (!option.exercised)
                    "The exercised-on date is null" using (option.exercisedOnDate == null)
                    "The spot price at purchase matches the oracle's data" using
                            (option.spotPriceAtPurchase == oracleCmd.value.spotPrice.value)

                    "The amount of cash transferred matches the premium" using
                            (premium == cashTransferredToIssuer.withoutIssuer())

                    "The time-window is no longer than 120 seconds" using
                            (Duration.between(timeWindow.fromTime, timeWindow.untilTime) <= Duration.ofSeconds(120))

                    // Constraints on the required signers.
                    "The issue command requires the issuer's signature" using (option.issuer.owningKey in command.signers)
                    "The issue command requires the owner's signature" using (option.owner.owningKey in command.signers)
                    // We can't check for the presence of the oracle as a required signer, as their identity is not
                    // included in the transaction. We check for the oracle as a required signer in the flow instead.

                    // We delegate some checks to the Cash contract:
                    // - Whether the input cash's owner has signed
                    // - Whether the value of cash inputs matches the value of cash outputs
                }
            }

            is Commands.Trade -> {
                requireThat {
                    // Constraints on the "shape" of the transaction.
                    val cashInputs = tx.inputsOfType<Cash.State>()
                    val cashOutputs = tx.outputsOfType<Cash.State>()
                    "Cash.State inputs are consumed" using (cashInputs.isNotEmpty())
                    "Cash.State outputs are created" using (cashOutputs.isNotEmpty())
                    "An OptionState input is consumed" using (tx.outputsOfType<OptionState>().size == 1)
                    "An OptionState output is created" using (tx.outputsOfType<OptionState>().size == 1)
                    "No other states are consumed" using (tx.inputs.size == (cashInputs.size + 1))
                    "No other states are created" using (tx.outputs.size == (cashOutputs.size + 1))
                    "Option issuances must be timestamped" using (tx.timeWindow != null)
                    tx.commands.requireSingleCommand<Cash.Commands.Move>()
                    val oracleCmd = tx.commands.requireSingleCommand<OptionContract.OracleCommand>()

                    // Constraints on the contents of the transaction's components.
                    val inputOption = tx.inputsOfType<OptionState>().single()
                    val outputOption = tx.outputsOfType<OptionState>().single()
                    val cashTransferredToOldOwner = tx.outputsOfType<Cash.State>().sumCashBy(inputOption.owner)
                    val timeWindow = tx.timeWindow!!
                    val premium = OptionState.calculatePremium(outputOption, oracleCmd.value.volatility)

                    "The owner has changed" using (inputOption.owner != outputOption.owner)
                    "The spot price at purchase matches the oracle's data" using
                            (outputOption.spotPriceAtPurchase == oracleCmd.value.spotPrice.value)
                    "The options are otherwise identical" using
                            (inputOption == outputOption.copy(owner = inputOption.owner, spotPriceAtPurchase = inputOption.spotPriceAtPurchase))

                    "The amount of cash transferred matches the premium" using
                            (premium == cashTransferredToOldOwner.withoutIssuer())

                    "The time-window is no longer than 120 seconds" using
                            (Duration.between(timeWindow.fromTime, timeWindow.untilTime) <= Duration.ofSeconds(120))

                    // Constraints on the required signers.
                    "The transfer command requires the old owner's signature" using (inputOption.owner.owningKey in command.signers)
                    "The transfer command requires the new owner's signature" using (outputOption.owner.owningKey in command.signers)
                }
            }

            is Commands.Exercise -> {
                requireThat {
                    // Constraints on the "shape" of the transaction.
                    "An OptionState is consumed" using (tx.inputsOfType<OptionState>().size == 1)
                    "No other inputs are consumed" using (tx.inputs.size == 1)
                    "A new OptionState is created" using (tx.outputsOfType<OptionState>().size == 1)
                    "No other states are created" using (tx.outputs.size == 1)
                    "Exercises of options must be timestamped" using (tx.timeWindow?.fromTime != null)

                    // Constraints on the contents of the transaction's components.
                    val input = tx.inputsOfType<OptionState>().single()
                    val output = tx.outputsOfType<OptionState>().single()
                    val timeWindow = tx.timeWindow!!

                    "The input option is not exercised" using (!input.exercised)
                    "The input option doesn't have an exercised-on date" using (input.exercisedOnDate == null)
                    "The output option is exercised" using (output.exercised)
                    "The output option has an exercised-on date" using (output.exercisedOnDate != null)
                    "The options are otherwise identical" using
                            (input == output.copy(exercised = false, exercisedOnDate = null))

                    "The option is being exercised before maturity" using
                            (tx.timeWindow!!.untilTime!! <= input.expiryDate)
                    "The output option's exercise data is within the time-window" using
                            (output.exercisedOnDate!! in timeWindow)
                    "The time-window is no longer than 120 seconds" using
                            (Duration.between(timeWindow.fromTime, timeWindow.untilTime) <= Duration.ofSeconds(120))

                    // Constraints on the required signers.
                    "The exercise command requires the owner's signature" using (input.owner.owningKey in command.signers)
                }
            }

            else -> throw IllegalArgumentException("Unknown command.")
        }
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Trade : TypeOnlyCommandData(), Commands
        class Exercise : TypeOnlyCommandData(), Commands
        // TODO: Do not delete. Will be used in the implementation of the redeem flow.
        class Redeem : TypeOnlyCommandData(), Commands
    }

    class OracleCommand(val spotPrice: SpotPrice, val volatility: Volatility) : CommandData
}