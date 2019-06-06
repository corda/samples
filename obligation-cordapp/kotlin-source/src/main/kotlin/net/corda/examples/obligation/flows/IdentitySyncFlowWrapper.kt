package net.corda.examples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * A simple wrapper of IdentitySyncFlow to make it standalone with its own flow session.
 * It turns out to be the same as the wrapper in IdentitySyncFlowTests.
 */
object IdentitySyncFlowWrapper {
    @InitiatingFlow
    class Initiator(val otherParty: Party,
                    val tx: WireTransaction,
                    override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Boolean>() {
        companion object {
            object SYNCING_WRAPPER : ProgressTracker.Step("Syncing Wrapper") {
                override fun childProgressTracker() = IdentitySyncFlow.Send.tracker()
            }

            fun tracker() = ProgressTracker(SYNCING_WRAPPER)
        }
        @Suspendable
        override fun call(): Boolean {
            val otherSideSession = initiateFlow(otherParty)
            subFlow(IdentitySyncFlow.Send(setOf(otherSideSession), tx, SYNCING_WRAPPER.childProgressTracker()))
            return otherSideSession.receive<Boolean>().unwrap { it }
        }
    }

    @InitiatedBy(Initiator::class)
    class Receive(val otherSideSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            subFlow(IdentitySyncFlow.Receive(otherSideSession))
            otherSideSession.send(true)
        }
    }
}