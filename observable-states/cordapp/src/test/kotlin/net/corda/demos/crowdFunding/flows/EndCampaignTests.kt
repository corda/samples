package net.corda.demos.crowdFunding.flows

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import net.corda.finance.GBP
import net.corda.finance.POUNDS
import net.corda.finance.contracts.getCashBalance
import net.corda.testing.node.StartedMockNode
import org.junit.Test
import kotlin.test.assertEquals

// TODO: Refactor repeated tests.
class EndCampaignTests : CrowdFundingTest() {
    private val rogersCampaign
        get() = Campaign(
                name = "Roger's Campaign",
                target = 1000.POUNDS,
                manager = a.legalIdentity(),
                deadline = fiveSecondsFromNow
        )

    private fun checkUpdatesAreCommitted(party: StartedMockNode, campaignId: UniqueIdentifier, campaignState: Campaign) {
        // Check that the EndCampaign transaction is committed by b and the Pledge/Campaign states are consumed.
        party.transaction {
            val (_, observable) = party.services.validatedTransactions.track()
            observable.first { it.tx.outputStates.isEmpty() }.subscribe { logger.info(it.tx.toString()) }

            val campaignQuery = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(campaignId))
            assertEquals(emptyList(), party.services.vaultService.queryBy<Campaign>(campaignQuery).states)
        }
    }

    // TODO: Finish this unit test.
    @Test
    fun `start campaign, make a pledge, don't raise enough, then end the campaign with a failure`() {
        // Start a campaign on PartyA.
        val startCampaignFlow = StartCampaign(rogersCampaign)
        val newCampaign = a.startFlow(startCampaignFlow).getOrThrow()
        network.waitQuiescent()

        val newCampaignState = newCampaign.tx.outputs.single().data as Campaign
        val newCampaignId = newCampaignState.linearId

        // B makes a pledge to A's campaign.
        val makePledgeFlow = MakePledge(100.POUNDS, newCampaignId)
        val campaignAfterFirstPledge = b.startFlow(makePledgeFlow).getOrThrow()
        network.waitQuiescent()

        val campaignStateAfterFirstPledge = campaignAfterFirstPledge.tx.outputsOfType<Campaign>().single()

        checkUpdatesAreCommitted(a, newCampaignId, campaignStateAfterFirstPledge)
        checkUpdatesAreCommitted(b, newCampaignId, campaignStateAfterFirstPledge)
        checkUpdatesAreCommitted(c, newCampaignId, campaignStateAfterFirstPledge)
        checkUpdatesAreCommitted(d, newCampaignId, campaignStateAfterFirstPledge)
        checkUpdatesAreCommitted(e, newCampaignId, campaignStateAfterFirstPledge)
    }

    @Test
    fun `start campaign, make a pledge, raise enough, then end the campaign with a success`() {
        // Issue cash to begin with.
        selfIssueCash(b, 500.POUNDS)
        selfIssueCash(c, 500.POUNDS)
        // Start a campaign on PartyA.
        val startCampaignFlow = StartCampaign(rogersCampaign)
        val newCampaign = a.startFlow(startCampaignFlow).getOrThrow()
        network.waitQuiescent()

        val newCampaignState = newCampaign.tx.outputs.single().data as Campaign
        val newCampaignStateRef = newCampaign.tx.outRef<Campaign>(0).ref
        val newCampaignId = newCampaignState.linearId

        logger.info("New campaign started")
        logger.info(newCampaign.toString())
        logger.info(newCampaign.tx.toString())

        // B makes a pledge to A's campaign.
        val bMakePledgeFlow = MakePledge(500.POUNDS, newCampaignId)
        val campaignAfterFirstPledge = b.startFlow(bMakePledgeFlow).getOrThrow()
        network.waitQuiescent()

        val campaignStateAfterFirstPledge = campaignAfterFirstPledge.tx.outputsOfType<Campaign>().single()
        val campaignStateRefAfterFirstPledge = campaignAfterFirstPledge.tx.outRefsOfType<Campaign>().single().ref
        val firstPledge = campaignAfterFirstPledge.tx.outputsOfType<Pledge>().single()

        logger.info("PartyB pledges £500 to PartyA")
        logger.info(campaignAfterFirstPledge.toString())
        logger.info(campaignAfterFirstPledge.tx.toString())

        // We need this to avoid double spend exceptions.
        Thread.sleep(1000)

        // C makes a pledge to A's campaign.
        val cMakePledgeFlow = MakePledge(500.POUNDS, newCampaignId)
        val campaignAfterSecondPledge = c.startFlow(cMakePledgeFlow).getOrThrow()
        network.waitQuiescent()

        val campaignStateAfterSecondPledge = campaignAfterSecondPledge.tx.outputsOfType<Campaign>().single()
        val campaignStateRefAfterSecondPledge = campaignAfterSecondPledge.tx.outRefsOfType<Campaign>().single().ref
        val secondPledge = campaignAfterSecondPledge.tx.outputsOfType<Pledge>().single()

        logger.info("PartyC pledges £500 to PartyA")
        logger.info(campaignAfterSecondPledge.toString())
        logger.info(campaignAfterSecondPledge.tx.toString())

        logger.info("PartyA runs the EndCampaign flow and requests cash from the pledgers (PartyB and PartyC).")
        a.transaction {
            val (_, observable) = a.services.validatedTransactions.track()
            observable.subscribe { tx ->
                // Don't log dependency transactions.
                val myKeys = a.services.keyManagementService.filterMyKeys(tx.tx.requiredSigningKeys).toList()
                if (myKeys.isNotEmpty()) {
                    logger.info(tx.tx.toString())
                }
            }
        }

        network.waitQuiescent()

        // Now perform the tests to check everyone has the correct data.

        // See that everyone gets the new campaign.
        val aNewCampaign = a.transaction { a.services.loadState(newCampaignStateRef).data }
        val bNewCampaign = b.transaction { b.services.loadState(newCampaignStateRef).data }
        val cNewCampaign = c.transaction { c.services.loadState(newCampaignStateRef).data }
        val dNewCampaign = d.transaction { d.services.loadState(newCampaignStateRef).data }
        val eNewCampaign = e.transaction { e.services.loadState(newCampaignStateRef).data }

        assertEquals(1, setOf(newCampaignState, aNewCampaign, bNewCampaign, cNewCampaign, dNewCampaign, eNewCampaign).size)

        // See that everyone gets the updated campaign after the first pledge.
        val aCampaignAfterPledge = a.transaction { a.services.loadState(campaignStateRefAfterFirstPledge).data }
        val bCampaignAfterPledge = b.transaction { b.services.loadState(campaignStateRefAfterFirstPledge).data }
        val cCampaignAfterPledge = c.transaction { c.services.loadState(campaignStateRefAfterFirstPledge).data }
        val dCampaignAfterPledge = d.transaction { d.services.loadState(campaignStateRefAfterFirstPledge).data }
        val eCampaignAfterPledge = e.transaction { e.services.loadState(campaignStateRefAfterFirstPledge).data }

        // All parties should have the same updated Campaign state.
        assertEquals(1, setOf(campaignStateAfterFirstPledge, aCampaignAfterPledge, bCampaignAfterPledge, cCampaignAfterPledge, dCampaignAfterPledge, eCampaignAfterPledge).size)

        // See that everyone gets the updated campaign after the second pledge.
        val aCampaignAfterSecondPledge = a.transaction { a.services.loadState(campaignStateRefAfterSecondPledge).data }
        val bCampaignAfterSecondPledge = b.transaction { b.services.loadState(campaignStateRefAfterSecondPledge).data }
        val cCampaignAfterSecondPledge = c.transaction { c.services.loadState(campaignStateRefAfterSecondPledge).data }
        val dCampaignAfterSecondPledge = d.transaction { d.services.loadState(campaignStateRefAfterSecondPledge).data }
        val eCampaignAfterSecondPledge = e.transaction { e.services.loadState(campaignStateRefAfterSecondPledge).data }

        // All parties should have the same updated Campaign state.
        assertEquals(1, setOf(campaignStateAfterSecondPledge, aCampaignAfterSecondPledge, bCampaignAfterSecondPledge, cCampaignAfterSecondPledge, dCampaignAfterSecondPledge, eCampaignAfterSecondPledge).size)

        // WARNING: The nodes which were not involved in the pledging or the campaign get to see the transferred cash in their vaults.
        // This is not a bug but a consequence of storing ALL output states in a transaction.
        // We need to change this such that a filtered transaction can be recorded instead of a full SignedTransaction.
        // The other option is not to broadcast the pledge transactions.
        a.transaction { logger.info(a.services.getCashBalance(GBP).toString()) }
        b.transaction { logger.info(b.services.getCashBalance(GBP).toString()) }
        c.transaction { logger.info(c.services.getCashBalance(GBP).toString()) }
        d.transaction { logger.info(d.services.getCashBalance(GBP).toString()) }
        e.transaction { logger.info(e.services.getCashBalance(GBP).toString()) }
    }

}