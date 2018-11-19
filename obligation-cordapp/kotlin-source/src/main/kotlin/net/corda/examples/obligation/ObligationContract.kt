package net.corda.examples.obligation

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.utils.sumCash
import java.security.PublicKey

class ObligationContract : Contract {

    companion object {
        @JvmStatic
        val OBLIGATION_CONTRACT_ID = "net.corda.examples.obligation.ObligationContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction): Unit {
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.Issue -> verifyIssue(tx, setOfSigners)
            is Commands.Transfer -> verifyTransfer(tx, setOfSigners)
            is Commands.Settle -> verifySettle(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun keysFromParticipants(obligation: Obligation): Set<PublicKey> {
        return obligation.participants.map {
            it.owningKey
        }.toSet()
    }

    // This only allows one obligation issuance per transaction.
    private fun verifyIssue(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs should be consumed when issuing an obligation." using (tx.inputStates.isEmpty())
        "Only one obligation state should be created when issuing an obligation." using (tx.outputStates.size == 1)
        val obligation = tx.outputsOfType<Obligation>().single()
        "A newly issued obligation must have a positive amount." using (obligation.amount.quantity > 0)
        "The lender and borrower cannot be the same identity." using (obligation.borrower != obligation.lender)
        "Both lender and borrower together only may sign obligation issue transaction." using
                (signers == keysFromParticipants(obligation))
    }

    // This only allows one obligation transfer per transaction.
    private fun verifyTransfer(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "An obligation transfer transaction should only consume one input state." using (tx.inputs.size == 1)
        "An obligation transfer transaction should only create one output state." using (tx.outputs.size == 1)
        val input = tx.inputsOfType<Obligation>().single()
        val output = tx.outputsOfType<Obligation>().single()
        "Only the lender property may change." using (input.withoutLender() == output.withoutLender())
        "The lender property must change in a transfer." using (input.lender != output.lender)
        "The borrower, old lender and new lender only must sign an obligation transfer transaction" using
                (signers == (keysFromParticipants(input) `union` keysFromParticipants(output)))
    }

    private fun verifySettle(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Check for the presence of an input obligation state.
        val obligationInputs = tx.inputsOfType<Obligation>()
        "There must be one input obligation." using (obligationInputs.size == 1)

        // Check there are output cash states.
        // We don't care about cash inputs, the Cash contract handles those.
        val cash = tx.outputsOfType<Cash.State>()
        "There must be output cash." using (cash.isNotEmpty())

        // Check that the cash is being assigned to us.
        val inputObligation = obligationInputs.single()
        val acceptableCash = cash.filter { it.owner == inputObligation.lender }
        "There must be output cash paid to the recipient." using (acceptableCash.isNotEmpty())

        // Sum the cash being sent to us (we don't care about the issuer).
        val sumAcceptableCash = acceptableCash.sumCash().withoutIssuer()
        val amountOutstanding = inputObligation.amount - inputObligation.paid
        "The amount settled cannot be more than the amount outstanding." using (amountOutstanding >= sumAcceptableCash)

        val obligationOutputs = tx.outputsOfType<Obligation>()

        // Check to see if we need an output obligation or not.
        if (amountOutstanding == sumAcceptableCash) {
            // If the obligation has been fully settled then there should be no obligation output state.
            "There must be no output obligation as it has been fully settled." using (obligationOutputs.isEmpty())
        } else {
            // If the obligation has been partially settled then it should still exist.
            "There must be one output obligation." using (obligationOutputs.size == 1)

            // Check only the paid property changes.
            val outputObligation = obligationOutputs.single()
            "The amount may not change when settling." using (inputObligation.amount == outputObligation.amount)
            "The borrower may not change when settling." using (inputObligation.borrower == outputObligation.borrower)
            "The lender may not change when settling." using (inputObligation.lender == outputObligation.lender)
            "The linearId may not change when settling." using (inputObligation.linearId == outputObligation.linearId)

            // Check the paid property is updated correctly.
            "Paid property incorrectly updated." using (outputObligation.paid == inputObligation.paid + sumAcceptableCash)
        }

        // Checks the required parties have signed.
        "Both lender and borrower together only must sign obligation settle transaction." using
                (signers == keysFromParticipants(inputObligation))
    }
}