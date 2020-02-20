package net.corda.examples.whistleblower.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.examples.whistleblower.states.BlowWhistleState

const val BLOW_WHISTLE_CONTRACT_ID = "net.corda.examples.whistleblower.contracts.BlowWhistleContract"

/**
 * A contract supporting two state transitions:
 * - Blowing the whistle on a company
 * - Transferring an existing case to a new investigator
 */
class BlowWhistleContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<Commands>()
        when (cmd.value) {
            is Commands.BlowWhistleCmd -> requireThat {
                "A BlowWhistle transaction should have zero inputs." using (tx.inputs.isEmpty())
                "A BlowWhistle transaction should have a BlowWhistleState output." using (tx.outputsOfType<BlowWhistleState>().size == 1)
                "A BlowWhistle transaction should have no other outputs." using (tx.outputs.size == 1)

                val output = tx.outputsOfType<BlowWhistleState>().single()
                "A BlowWhistle transaction should be signed by the whistle-blower and the investigator." using
                        (cmd.signers.containsAll(output.participants.map { it.owningKey }))
            }
        }
    }

    sealed class Commands : CommandData {
        /** Blowing the whistle on a company. */
        class BlowWhistleCmd : Commands()
    }
}