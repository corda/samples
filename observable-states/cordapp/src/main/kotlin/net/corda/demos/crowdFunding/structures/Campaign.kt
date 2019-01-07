package net.corda.demos.crowdFunding.structures

import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.demos.crowdFunding.contracts.CampaignContract
import net.corda.demos.crowdFunding.flows.EndCampaign
import java.time.Instant
import java.util.*

@BelongsToContract(CampaignContract::class)
data class Campaign(
        val name: String,
        val manager: Party,
        val target: Amount<Currency>,
        val deadline: Instant,
        val raisedSoFar: Amount<Currency> = Amount(0, target.token),
        override val participants: List<AbstractParty> = listOf(manager),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, SchedulableState {
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create(EndCampaign.Initiator::class.java, thisStateRef), deadline)
    }
}