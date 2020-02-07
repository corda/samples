package com.observable.flows

import co.paralleluniverse.fibers.Suspendable
import com.observable.contracts.HighlyRegulatedContract
import com.observable.states.HighlyRegulatedState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TradeAndReport(val buyer: Party, val stateRegulator: Party, val nationalRegulator: Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(HighlyRegulatedState(buyer, ourIdentity), HighlyRegulatedContract.ID)
                .addCommand(HighlyRegulatedContract.Commands.Trade(), ourIdentity.owningKey)

        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(initiateFlow(buyer), initiateFlow(stateRegulator))
        // We distribute the transaction to both the buyer and the state regulator using `FinalityFlow`.
        subFlow(FinalityFlow(signedTransaction, sessions))

        // We also distribute the transaction to the national regulator manually.
        subFlow(ReportManually(signedTransaction, nationalRegulator))
    }
}

@InitiatedBy(TradeAndReport::class)
class TradeAndReportResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Both the buyer and the state regulator record all of the transaction's states using
        // `ReceiveFinalityFlow` with the `ALL_VISIBLE` flag.
        subFlow(ReceiveFinalityFlow(counterpartySession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}
