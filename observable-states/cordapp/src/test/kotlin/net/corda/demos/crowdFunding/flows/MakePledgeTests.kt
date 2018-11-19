package net.corda.demos.crowdFunding.flows

import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.utilities.getOrThrow
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import net.corda.finance.POUNDS
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

        // Extract the state from the transaction.
        val campaignState = createCampaignTransaction.tx.outputs.single().data as Campaign
        val campaignId = campaignState.linearId

        // Make a pledge from PartyB to PartyA for £100.
        val makePledgeFlow = MakePledge.Initiator(100.POUNDS, campaignId, broadcastToObservers = true)
        val acceptPledgeTransaction = b.startFlow(makePledgeFlow).getOrThrow()

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

        // Only A and B should know the identity of the pledger (who is B in this case).
        assertEquals(b.legalIdentity(), a.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))
        assertEquals(b.legalIdentity(), b.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))
        assertEquals(null, c.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))
        assertEquals(null, d.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))
        assertEquals(null, e.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))

        network.waitQuiescent()
    }

    @Test
    fun `successfully make a pledge without broadcasting the updated campaign state to all parties`() {
        // Campaign.
        val rogersCampaign = Campaign(
                name = "Roger's Campaign",
                target = 1000.POUNDS,
                manager = a.legalIdentity(),
                deadline = fiveSecondsFromNow // We shut the nodes down before the EndCampaignFlow is run.
        )

        // Start a new campaign.
        val startCampaignFlow = StartCampaign(rogersCampaign)
        val createCampaignTransaction = a.startFlow(startCampaignFlow).getOrThrow()

        // Extract the state from the transaction.
        val campaignState = createCampaignTransaction.tx.outputs.single().data as Campaign
        val campaignId = campaignState.linearId

        // Make a pledge from PartyB to PartyA for £100 but don't broadcast it to everyone else.
        val makePledgeFlow = MakePledge.Initiator(100.POUNDS, campaignId, broadcastToObservers = false)
        val acceptPledgeTransaction = b.startFlow(makePledgeFlow).getOrThrow()

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
        assertFailsWith(TransactionResolutionException::class) { c.transaction { c.services.loadState(campaignStateRefAfterPledge) } }
        assertFailsWith(TransactionResolutionException::class) { d.transaction { d.services.loadState(campaignStateRefAfterPledge) } }
        assertFailsWith(TransactionResolutionException::class) { e.transaction { e.services.loadState(campaignStateRefAfterPledge) } }

        // Only PartyA and PartyB should have the updated campaign state.
        assertEquals(1, setOf(campaignAfterPledge, aCampaignAfterPledge, bCampaignAfterPledge).size)

        val aNewPledge = a.transaction { a.services.loadState(newPledgeStateRef).data } as Pledge
        val bNewPledge = b.transaction { b.services.loadState(newPledgeStateRef).data } as Pledge
        assertFailsWith(TransactionResolutionException::class) { c.transaction { c.services.loadState(newPledgeStateRef) } }
        assertFailsWith(TransactionResolutionException::class) { d.transaction { d.services.loadState(newPledgeStateRef) } }
        assertFailsWith(TransactionResolutionException::class) { e.transaction { e.services.loadState(newPledgeStateRef) } }

        // Only PartyA and PartyB should have the updated campaign state.
        assertEquals(1, setOf(newPledge, aNewPledge, bNewPledge).size)

        // Only A and B should know the identity of the pledger (who is B in this case). Of course, the others won't know.
        assertEquals(b.legalIdentity(), a.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))
        assertEquals(b.legalIdentity(), b.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger))
        assertEquals(null, c.transaction { c.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger) })
        assertEquals(null, d.transaction { d.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger) })
        assertEquals(null, e.transaction { e.services.identityService.wellKnownPartyFromAnonymous(newPledge.pledger) })

        network.waitQuiescent()
    }
}