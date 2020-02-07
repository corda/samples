package com.observable.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class HighlyRegulatedContract : Contract {
    companion object {
        const val ID = "com.observable.contracts.HighlyRegulatedContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // The contract logic is irrelevant for this sample.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Trade : Commands
    }
}