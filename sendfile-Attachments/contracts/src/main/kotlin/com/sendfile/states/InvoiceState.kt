package com.sendfile.states

import com.sendfile.contracts.InvoiceContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

// *********
// * State *
// *********
@BelongsToContract(InvoiceContract::class)
data class InvoiceState(
        val incoiceAttachmentID: String,
        override val participants: List<AbstractParty> = listOf()) : ContractState
