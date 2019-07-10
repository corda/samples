package net.corda.yo.state

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import net.corda.yo.contract.YoContract


// State.
@BelongsToContract(YoContract::class)
data class YoState(val origin: Party,
                   val target: Party,
                   val yo: String = "Yo!") : ContractState {
    override val participants = listOf(target)
    override fun toString() = "${origin.name}: $yo"
}
