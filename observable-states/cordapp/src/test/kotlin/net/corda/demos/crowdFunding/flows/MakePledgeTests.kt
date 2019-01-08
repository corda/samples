package net.corda.demos.crowdFunding.flows

import net.corda.core.utilities.getOrThrow
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import net.corda.finance.POUNDS
import org.junit.Test
import kotlin.test.assertEquals

class MakePledgeTests : CrowdFundingTest() {
    @Test
    fun `successfully make a pledge and broadcast the updated campaign state to all parties`() {
        // Campaign.
        val rogersCampaign = Campaign(
                name = "Roger's Campaign",
                target = 1000.POUNDS,
                manager = a.legalIdentity(),
                deadline = fiveSecondsFromNow
        )

        // Start a new campaign.
        val startCampaignFlow = StartCampaign(rogersCampaign)
        val createCampaignTransaction = a.startFlow(startCampaignFlow).getOrThrow()
        network.waitQuiescent()

        // Extract the state from the transaction.
        val campaignState = createCampaignTransaction.tx.outputs.single().data as Campaign
        val campaignId = campaignState.linearId

        // Make a pledge from PartyB to PartyA for £100.
        val makePledgeFlow = MakePledge(100.POUNDS, campaignId)
        val acceptPledgeTransaction = b.startFlow(makePledgeFlow).getOrThrow()
        network.waitQuiescent()

        logger.info("New campaign started")
        logger.info(createCampaignTransaction.toString())
        logger.info(createCampaignTransaction.tx.toString())

        logger.info("PartyB pledges £100 to PartyA")
        logger.info(acceptPledgeTransaction.toString())
        logger.info(acceptPledgeTransaction.tx.toString())

        //Extract the states from the transaction.
        val campaignStateRefAfterPledge = acceptPledgeTransaction.tx.outRefsOfType<Campaign>().single().ref
        val campaignAfterPledge = acceptPledgeTransaction.tx.outputsOfType<Campaign>().single()
        val newPledgeStateRef = acceptPledgeTransaction.tx.outRefsOfType<Pledge>().single().ref
        val newPledge = acceptPledgeTransaction.tx.outputsOfType<Pledge>().single()

        val aCampaignAfterPledge = a.transaction { a.services.loadState(campaignStateRefAfterPledge).data }
        val bCampaignAfterPledge = b.transaction { b.services.loadState(campaignStateRefAfterPledge).data }
        val cCampaignAfterPledge = c.transaction { c.services.loadState(campaignStateRefAfterPledge).data }
        val dCampaignAfterPledge = d.transaction { d.services.loadState(campaignStateRefAfterPledge).data }
        val eCampaignAfterPledge = e.transaction { e.services.loadState(campaignStateRefAfterPledge).data }

        // All parties should have the same updated Campaign state.
        assertEquals(1,
                setOf(
                        campaignAfterPledge,
                        aCampaignAfterPledge,
                        bCampaignAfterPledge,
                        cCampaignAfterPledge,
                        dCampaignAfterPledge,
                        eCampaignAfterPledge
                ).size
        )

        val aNewPledge = a.transaction { a.services.loadState(newPledgeStateRef).data } as Pledge
        val bNewPledge = b.transaction { b.services.loadState(newPledgeStateRef).data } as Pledge
        val cNewPledge = c.transaction { c.services.loadState(newPledgeStateRef).data } as Pledge
        val dNewPledge = d.transaction { d.services.loadState(newPledgeStateRef).data } as Pledge
        val eNewPledge = e.transaction { e.services.loadState(newPledgeStateRef).data } as Pledge

        // All parties should have the same Pledge state.
        assertEquals(1,
                setOf(
                        newPledge,
                        aNewPledge,
                        bNewPledge,
                        cNewPledge,
                        dNewPledge,
                        eNewPledge
                ).size
        )
    }
}