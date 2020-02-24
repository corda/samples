package net.corda.examples.autopayroll.states

import net.corda.examples.autopayroll.contracts.MoneyStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(MoneyStateContract::class)
data class MoneyState(
        val amount: Int,
        val receiver:Party,
        override val participants: List<AbstractParty> = listOf(receiver)) : ContractState
