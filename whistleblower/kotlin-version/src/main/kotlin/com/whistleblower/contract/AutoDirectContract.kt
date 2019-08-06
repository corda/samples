package com.whistleblower.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

const val AUTODIRECT_ID = "com.whistleblower.contract.AutoDirectContract"

/**
 * A contract supporting two state transitions:
 * - Blowing the whistle on a company
 * - Transferring an existing case to a new investigator
 */
class AutoDirectContract : Contract {
    override fun verify(tx: LedgerTransaction) {
    }

    sealed class Commands : CommandData {
        /** Blowing the whistle on a company. */
        class  AutoDirect : Commands()
    }
}