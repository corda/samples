package com.example.contract

import com.example.state.InvoiceState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [InvoiceState], which in turn encapsulates an [InvoiceState].
 *
 * For a new [InvoiceState] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [InvoiceState].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class InvoiceContract : Contract {
    companion object {
        @JvmStatic
        val ID = "com.example.contract.InvoiceContract"
    }

    // Commands signed by oracles must contain the facts the oracle is attesting to.
    interface Commands : CommandData {
        class Create(val contractor: Party, val company: Party, val rate: Double) : Commands
        class Pay : Commands
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when(command.value) {
            is Commands.Create -> {
                requireThat {
                    // Generic constraints around the invoice transaction.
                    "No inputs should be consumed when issuing an Invoice." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<InvoiceState>().single()
                    "The lender and the borrower cannot be the same entity." using (out.contractor != out.company)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                    // Invoice-specific constraints.
                    "The Invoice's value must be non-negative. (was ${out.hoursWorked})" using (out.hoursWorked > 0)
                    "The invoice should not yet be paid." using (!out.paid)
                }
            }
            is Commands.Pay -> {
                requireThat {
                    "Exactly one invoice state should be consumed when paying an Invoice." using (tx.inputsOfType<InvoiceState>().size == 1)
                    "Expect only one invoice state output from paying an invoice." using(tx.outputsOfType<InvoiceState>().size == 1)
                    "Expect at least one output in addition to invoice." using (tx.outputStates.size >= 2)

                    // Invoice-specific constraints
                    // TODO: Support partial payment
                    val outInvoice = tx.outputsOfType<InvoiceState>().single()
                    "The invoice should now be paid." using (outInvoice.paid)
                }
            }
        }
    }
}
