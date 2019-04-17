package com.gitcoins.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

/**
 * Temporarily required until known bug is fixed.
 */
class GitTokenContract : Contract {
    interface Commands : CommandData {
        class Issue : Commands
    }
    override fun verify(tx: LedgerTransaction) {
    }
}
