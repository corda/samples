package com.heartbeat

import net.corda.core.contracts.SchedulableState
import net.corda.core.contracts.ScheduledActivity
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.Party
import java.time.Instant

/**
 * Every Heartbeat state has a scheduled activity to start a flow to consume itself and produce a
 * new Heartbeat state on the ledger after five seconds.
 *
 * @property me The creator of the Heartbeat state.
 * @property nextActivityTime When the scheduled activity should be kicked off.
 */
class HeartState(
        private val me: Party,
        private val nextActivityTime: Instant = Instant.now().plusSeconds(1)
) : SchedulableState {

    override val participants get() = listOf(me)

    // Defines the scheduled activity to be conducted by the SchedulableState.
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        // A heartbeat will be emitted every second.
        // We get the time when the scheduled activity will occur in the constructor rather than in this method. This is
        // because calling Instant.now() in nextScheduledActivity returns the time at which the function is called, rather
        // than the time at which the state was created.
        return ScheduledActivity(flowLogicRefFactory.create(HeartbeatFlow::class.java, thisStateRef), nextActivityTime)
    }

}