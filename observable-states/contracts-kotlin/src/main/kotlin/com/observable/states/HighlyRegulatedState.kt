package com.observable.states

import com.observable.contracts.HighlyRegulatedContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

@BelongsToContract(HighlyRegulatedContract::class)
data class HighlyRegulatedState(val buyer: Party, val seller: Party) : ContractState {
    override val participants = listOf(buyer, seller)
}
