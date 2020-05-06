package net.corda.examples.oracle.base.contract

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

const val PRIME_PROGRAM_ID: ContractClassName = "net.corda.examples.oracle.base.contract.PrimeContract"

class PrimeContract : Contract {
    // Commands signed by oracles must contain the facts the oracle is attesting to.
    class Create(val n: Int, val nthPrime: Int) : CommandData

    // Our contract does not check that the Nth prime is correct. Instead, it checks that the
    // information in the command and state match.
    override fun verify(tx: LedgerTransaction) = requireThat {
        "There are no inputs" using (tx.inputs.isEmpty())
        val output = tx.outputsOfType<PrimeState>().single()
        val command = tx.commands.requireSingleCommand<Create>().value
        "The prime in the output does not match the prime in the command." using
                (command.n == output.n && command.nthPrime == output.nthPrime)
    }
}

// If 'n' is a natural number N then 'nthPrime' is the Nth prime.
// `Requester` is the Party that will store this fact in its vault.
@BelongsToContract(PrimeContract::class)
data class PrimeState(val n: Int,
                      val nthPrime: Int,
                      val requester: AbstractParty) : ContractState {
    override val participants: List<AbstractParty> get() = listOf(requester)
    override fun toString() = "The ${n}th prime number is $nthPrime."
}