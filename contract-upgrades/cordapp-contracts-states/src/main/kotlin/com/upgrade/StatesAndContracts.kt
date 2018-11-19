package com.upgrade

import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

data class State(val a: AbstractParty, val b: AbstractParty) : ContractState {
    override val participants get() = listOf(a, b)
}

open class OldContract : Contract {
    companion object {
        val id = "com.upgrade.OldContract"
    }

    override fun verify(tx: LedgerTransaction) {}

    class Action : TypeOnlyCommandData()
}