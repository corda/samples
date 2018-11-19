package com.upgrade

import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UpgradedContract
import net.corda.core.transactions.LedgerTransaction

// Upgraded contracts must implement the UpgradedContract interface.
// We're not upgrading the state, so we pass the same state as the input and output state.
open class NewContract : UpgradedContract<State, State> {
    companion object {
        val id = "com.upgrade.NewContract"
    }

    override val legacyContract = OldContract.id

    // Again, we're not upgrading the state, so we leave the states unmodified.
    override fun upgrade(state: State) = state

    override fun verify(tx: LedgerTransaction) {}

    class Action : TypeOnlyCommandData()
}