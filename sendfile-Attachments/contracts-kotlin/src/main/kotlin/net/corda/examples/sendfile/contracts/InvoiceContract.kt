package net.corda.examples.sendfile.contracts

import net.corda.examples.sendfile.states.InvoiceState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class InvoiceContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.examples.sendfile.contracts.InvoiceContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val cmd = tx.commands.requireSingleCommand<Commands>()
        when(cmd.value){
            is Commands.Issue -> requireThat {
                val output = tx.outputsOfType<InvoiceState>().single()
                "Attachment ID must be stored in state" using (output.invoiceAttachmentID.isNotEmpty())
            }
        }

    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
    }
}