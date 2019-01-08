package net.corda.demos.crowdFunding.flows

import net.corda.core.utilities.getOrThrow
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.finance.POUNDS
import org.junit.Test
import kotlin.test.assertEquals

class StartCampaignTests : CrowdFundingTest() {
    private val rogersCampaign
        get() = Campaign(
            name = "Roger's Campaign",
            target = 1000.POUNDS,
            manager = a.legalIdentity(),
            deadline = fiveSecondsFromNow
    )

    @Test
    fun `successfully start and broadcast campaign to all nodes`() {
        // Start a new Campaign.
        val flow = StartCampaign(rogersCampaign)
        val campaign = a.startFlow(flow).getOrThrow()

        network.waitQuiescent()

        // Extract the state from the transaction.
        val campaignStateRef = campaign.tx.outRef<Campaign>(0).ref
        val campaignState = campaign.tx.outputs.single()

        // Get the Campaign state from the observer node vaults.
        val aCampaign = a.transaction { a.services.loadState(campaignStateRef) }
        val bCampaign = b.transaction { b.services.loadState(campaignStateRef) }
        val cCampaign = c.transaction { c.services.loadState(campaignStateRef) }
        val dCampaign = d.transaction { d.services.loadState(campaignStateRef) }
        val eCampaign = e.transaction { e.services.loadState(campaignStateRef) }

        // All the states should be equal.
        assertEquals(1, setOf(campaignState, aCampaign, bCampaign, cCampaign, dCampaign, eCampaign).size)

        logger.info("Even though PartyA is the only participant in the Campaign, all other parties should have a copy of it.")
        logger.info("The Campaign state does not include any information about the observers.")
        logger.info("PartyA: $campaignState")
        logger.info("PartyB: $bCampaign")
        logger.info("PartyC: $cCampaign")
        logger.info("PartyD: $dCampaign")
        logger.info("PartyE: $eCampaign")
    }

}