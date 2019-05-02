package com.upgrade.legacy.constraint.new

import com.upgrade.old.OldContract
import com.upgrade.old.OldState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

// Upgraded contracts must implement the UpgradedContract interface.
class NewContractWithLegacyConstraint : UpgradedContractWithLegacyConstraint<OldState, NewState> {

    // SHA-256 hash of the JAR containing the old contract.
    override val legacyContractConstraint = HashAttachmentConstraint(SecureHash.parse("2B13EF694233556AD3CD86B0EF693BD40484C60A66599317DF244C6245D64E6A"))

    companion object {
        const val id = "com.upgrade.new.with.legacy.constraint.NewContractWithLegacyConstraint"
    }

    override val legacyContract = OldContract.id

    // Again, we're not upgrading the state, so we leave the states unmodified.
    override fun upgrade(state: OldState) = NewState(state.a, state.b)

    override fun verify(tx: LedgerTransaction) {}

    class Action : TypeOnlyCommandData()
}

@BelongsToContract(NewContractWithLegacyConstraint::class)
data class NewState(val a: AbstractParty, val b: AbstractParty) : ContractState {
    override val participants get() = listOf(a, b)
}
