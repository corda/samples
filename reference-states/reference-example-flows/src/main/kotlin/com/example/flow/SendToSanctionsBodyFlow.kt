package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord

/**
 * This flow allows the SanctionsBody to act as a regulator, so that IOUs can be sent to the Sanctions Body for review.
 *
 * This demonstrates how reference states work with Observer nodes.
 */
object SendToSanctionsBodyFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val sanctionsBody: Party,
                    val txId: SecureHash) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val tx = serviceHub.validatedTransactions.getTransaction(txId)!!
            val session = initiateFlow(sanctionsBody)
            subFlow(SendTransactionFlow(session, tx))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherSideSession: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(ReceiveTransactionFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        }
    }

}