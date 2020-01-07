package com.example.flow

import com.example.state.SanctionableIOUState
import com.example.state.SanctionedEntities
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.NotaryException
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.temporal.ChronoUnit

class IOUFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode
    lateinit var issuer: StartedMockNode
    lateinit var issuerParty: Party

    @Before
    fun setup() {
        network = MockNetwork(
            listOf("com.example.contract", "com.example.schema"), MockNetworkParameters(
                networkParameters = testNetworkParameters(
                    minimumPlatformVersion = 4
                )
            )
        )
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()

        issuer = network.createPartyNode()
        issuerParty = issuer.info.legalIdentities.single()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b, c).forEach { it.registerInitiatedFlow(IOUIssueFlow.Acceptor::class.java) }
        listOf(issuer).forEach { it.registerInitiatedFlow(GetSanctionsListFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test(expected = TransactionVerificationException.ContractRejection::class)
    fun `deal fails if there is no issued sanctions list`() {
        val flow = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)

        println(signedTx.coreTransaction.outputsOfType(SanctionableIOUState::class.java).single())
    }

    @Test
    fun `deal succeeds with issued sanctions`() {
        val issuanceFlow = issuer.startFlow(IssueSanctionsListFlow.Initiator())
        network.runNetwork()
        issuanceFlow.getOrThrow()

        getSanctionsList(a, issuerParty)

        val flow = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)

        println("@@@@@@@@@@${a.services.vaultService.queryBy(SanctionedEntities::class.java).states.firstOrNull()}")

    }

    @Test(expected = TransactionVerificationException.ContractRejection::class)
    fun `deal is rejected if party is sanctioned`() {
        val issuanceFlow = issuer.startFlow(IssueSanctionsListFlow.Initiator())
        network.runNetwork()
        issuanceFlow.getOrThrow()

        val updateFuture = issuer.startFlow(UpdateSanctionsListFlow.Initiator(b.info.legalIdentities.first()))
        network.runNetwork()
        updateFuture.getOrThrow()

        val flow = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val future = a.startFlow(flow)
        network.runNetwork()

        future.getOrThrow()
    }

    @Test(expected = NotaryException::class)
    fun `deal fails if list is updated after collection`() {
        val issuanceFlow = issuer.startFlow(IssueSanctionsListFlow.Initiator())
        network.runNetwork()
        issuanceFlow.getOrThrow()
        getSanctionsList(a, issuerParty)

        val flow1 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val future1 = a.startFlow(flow1)
        network.runNetwork()

        val signedTx1 = future1.getOrThrow()
        signedTx1.verifySignaturesExcept(b.info.singleIdentity().owningKey)

        val updateFuture = issuer.startFlow(UpdateSanctionsListFlow.Initiator(c.info.legalIdentities.first()))
        network.runNetwork()
        updateFuture.getOrThrow()

        val flow2 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val future2 = a.startFlow(flow2)
        network.runNetwork()

        future2.getOrThrow()
    }

    @Test
    fun `deal succeeds if list is collected again after update`() {
        val issuanceFlow = issuer.startFlow(IssueSanctionsListFlow.Initiator())
        network.runNetwork()
        issuanceFlow.getOrThrow()

        getSanctionsList(a, issuerParty)

        val dealFlow1 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val dealFuture1 = a.startFlow(dealFlow1)
        network.runNetwork()

        val signedTx1 = dealFuture1.getOrThrow()
        signedTx1.verifySignaturesExcept(b.info.singleIdentity().owningKey)

        val updateFuture = issuer.startFlow(UpdateSanctionsListFlow.Initiator(c.info.legalIdentities.first()))
        network.runNetwork()
        updateFuture.getOrThrow()

        //update on node a only
        val getUpdatedListAgain = a.startFlow(GetSanctionsListFlow.Initiator(issuerParty))
        network.runNetwork()
        getUpdatedListAgain.getOrThrow()

        val dealFlow2 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val dealFuture2 = a.startFlow(dealFlow2)
        network.runNetwork()
        dealFuture2.getOrThrow()

        //perform deal with b again
        val dealFlow3 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val dealFuture3 = a.startFlow(dealFlow3)
        network.runNetwork()

        dealFuture3.getOrThrow()

    }

    private fun getSanctionsList(node: StartedMockNode, issuerOfSanctions: Party) {
        node.startFlow(GetSanctionsListFlow.Initiator(issuerOfSanctions)).also {
            network.runNetwork()
        }.getOrThrow()
        network.runNetwork()
    }

    @Test
    fun `during tx resoludtion, latest ref state is provided to counterparty`() {
        val issuanceFlow = issuer.startFlow(IssueSanctionsListFlow.Initiator())
        network.runNetwork()
        issuanceFlow.getOrThrow()

        getSanctionsList(a, issuerParty)


        val dealFlow1 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val dealFuture1 = a.startFlow(dealFlow1)
        network.runNetwork()

        val signedTx1 = dealFuture1.getOrThrow()
        signedTx1.verifySignaturesExcept(b.info.singleIdentity().owningKey)

        val updateFuture = issuer.startFlow(UpdateSanctionsListFlow.Initiator(c.info.legalIdentities.first()))
        network.runNetwork()
        updateFuture.getOrThrow()

        //update on node a only
        getSanctionsList(a, issuerParty)


        val dealFlow2 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val dealFuture2 = a.startFlow(dealFlow2)
        network.runNetwork()
        dealFuture2.getOrThrow()

        //perform deal with b again
        val dealFlow3 = IOUIssueFlow.Initiator(1, b.info.singleIdentity(), issuerParty)
        val dealFuture3 = a.startFlow(dealFlow3)
        network.runNetwork()

        dealFuture3.getOrThrow()

        //take down issuer, so isn't able to provide new list to node b
        issuer.stop()

        val dealFlow4 = IOUIssueFlow.Initiator(1, a.info.singleIdentity(), issuerParty)
        val dealFuture4 = b.startFlow(dealFlow4)
        network.runNetwork()

        dealFuture4.getOrThrow(timeout = Duration.of(30, ChronoUnit.MINUTES))

    }
}