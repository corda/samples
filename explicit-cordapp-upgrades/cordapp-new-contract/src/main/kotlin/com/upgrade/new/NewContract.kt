package com.upgrade.new

import com.upgrade.old.OldContract
import com.upgrade.old.OldState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UpgradedContract
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

// Upgraded contracts must implement the UpgradedContract interface.
class NewContract : UpgradedContract<OldState, NewState> {
    companion object {
        const val id = "com.upgrade.new.NewContract"
    }

    override val legacyContract = OldContract.id

    // Again, we're not upgrading the state, so we leave the states unmodified.
    override fun upgrade(state: OldState) = NewState(state.a, state.b)

    override fun verify(tx: LedgerTransaction) {}

    class Action : TypeOnlyCommandData()
}

@BelongsToContract(NewContract::class)
data class NewState(val a: AbstractParty, val b: AbstractParty) : ContractState {
    override val participants get() = listOf(a, b)
}
