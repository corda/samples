package com.example.contract

import com.example.state.SanctionedEntities
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SanctionedEntitiesContract : Contract {
    companion object {
        @JvmStatic
        val SANCTIONS_CONTRACT_ID = "com.example.contract.SanctionedEntitiesContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType(Commands::class.java).single()

        if (command.value is Commands.Create) {
            requireThat {
                "when creating a sanctions list there should be no inputs" using (tx.inputStates.isEmpty())
                "when creating a sanctions list there should be one output" using (tx.outputsOfType(SanctionedEntities::class.java).size == 1)
                val out = tx.outputsOfType(SanctionedEntities::class.java).single()
                "The issuer of the sanctions list must sign" using (out.issuer.owningKey in command.signers)
            }

        } else if (command.value is Commands.Update) {
            requireThat {
                "There must be exactly one input Sanctions List when updating" using (tx.inputsOfType(SanctionedEntities::class.java).size == 1)
                "There must be exactly one output Sanctions List when updating" using (tx.outputsOfType(
                    SanctionedEntities::class.java
                ).size == 1)
                val input = tx.inputsOfType<SanctionedEntities>().single()
                val output = tx.outputsOfType<SanctionedEntities>().single()
                "The issuer must remain the same across an update" using (input.issuer == output.issuer)
            }
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        object Create : Commands
        object Update : Commands
    }
}
