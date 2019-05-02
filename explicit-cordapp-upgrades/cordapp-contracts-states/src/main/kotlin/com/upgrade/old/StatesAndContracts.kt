package com.upgrade.old

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

open class OldContract : Contract {
    companion object {
        const val id = "com.upgrade.old.OldContract"
    }

    override fun verify(tx: LedgerTransaction) {}

    class Action : TypeOnlyCommandData()
}

@BelongsToContract(OldContract::class)
data class OldState(val a: AbstractParty, val b: AbstractParty) : ContractState {
    override val participants get() = listOf(a, b)
}
