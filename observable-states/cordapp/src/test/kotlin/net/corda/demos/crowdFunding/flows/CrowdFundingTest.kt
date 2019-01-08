package net.corda.demos.crowdFunding.flows

import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.slf4j.Logger
import java.time.Instant
import java.util.*

abstract class CrowdFundingTest {
    protected lateinit var network: MockNetwork
    protected lateinit var a: StartedMockNode
    protected lateinit var b: StartedMockNode
    protected lateinit var c: StartedMockNode
    protected lateinit var d: StartedMockNode
    protected lateinit var e: StartedMockNode

    @Before
    fun setupNetwork() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.demos.crowdFunding"),
                TestCordapp.findCordapp("net.corda.finance")), threadPerNode = true))
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()
        d = network.createPartyNode()
        e = network.createPartyNode()
        listOf(a, b, c, d, e).forEach { node -> registerFlowsAndServices(node) }
    }

    @After
    fun tearDownNetwork() {
        network.stopNodes()
    }

    companion object {
        val logger: Logger = loggerFor<CrowdFundingTest>()
    }

    private fun calculateDeadlineInSeconds(interval: Long) = Instant.now().plusSeconds(interval)
    protected val fiveSecondsFromNow get() = calculateDeadlineInSeconds(5L)

    private fun registerFlowsAndServices(node: StartedMockNode) {
        node.registerInitiatedFlow(RecordCampaignStartAsObserver::class.java)
        node.registerInitiatedFlow(RecordPledgeAsObserver::class.java)
        node.registerInitiatedFlow(SettlePledge::class.java)
        node.registerInitiatedFlow(RecordPledgeAsObserver::class.java)
    }

    fun StartedMockNode.legalIdentity(): Party {
        return this.info.legalIdentities.first()
    }

    protected fun selfIssueCash(party: StartedMockNode,
                                amount: Amount<Currency>): SignedTransaction {
        val notary = party.services.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw IllegalStateException("Could not find a notary.")
        val issueRef = OpaqueBytes.of(0)
        val issueRequest = CashIssueFlow.IssueRequest(amount, issueRef, notary)
        val flow = CashIssueFlow(issueRequest)
        return party.startFlow(flow).getOrThrow().stx
    }
}
