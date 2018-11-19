package net.corda.examples.attachments.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

data class AgreementState(val partyA: Party, val partyB: Party, val txt: String) : ContractState {
    override val participants get() = listOf(partyA, partyB)
}