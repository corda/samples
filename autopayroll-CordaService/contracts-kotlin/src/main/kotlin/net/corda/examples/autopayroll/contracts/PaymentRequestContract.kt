package net.corda.examples.autopayroll.contracts

import net.corda.examples.autopayroll.states.PaymentRequestState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
// ************
// * Contract *
// ************
class PaymentRequestContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.examples.autopayroll.contracts.PaymentRequestContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val cmd = tx.commands.requireSingleCommand<Commands>()
        when(cmd.value){
            is Commands.Request -> requireThat {
                "The single output is of type PaymentRequestState" using (tx.outputsOfType<PaymentRequestState>().size == 1)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Request : Commands
    }
}