package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.flow.GetSanctionsListFlow.Acceptor
import com.example.flow.GetSanctionsListFlow.Initiator
import com.example.state.SanctionedEntities
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.unwrap

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object GetSanctionsListFlow {
    private val YES = "YES"

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val otherParty: Party) : FlowLogic<List<StateAndRef<SanctionedEntities>>>() {

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): List<StateAndRef<SanctionedEntities>> {
            val session = initiateFlow(otherParty)
            val resolve = session.receive(String::class.java).unwrap { it }
            return if (resolve == YES) {
                val newestSanctionsList = subFlow(ReceiveTransactionFlow(session, true, StatesToRecord.ALL_VISIBLE))
                newestSanctionsList.coreTransaction.outRefsOfType()
            } else {
                emptyList()
            }
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val listStateAndRefs = serviceHub.vaultService.queryBy(SanctionedEntities::class.java).states.firstOrNull()
            if (listStateAndRefs == null) {
                otherPartySession.send("NO")
            } else {
                otherPartySession.send(YES)
                subFlow(
                    SendTransactionFlow(
                        otherPartySession,
                        serviceHub.validatedTransactions.getTransaction(listStateAndRefs.ref.txhash)!!
                    )
                )
            }
        }
    }
}
