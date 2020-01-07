package com.example.flow

import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SanctionsFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.example.contract", "com.example.schema"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(IOUIssueFlow.Acceptor::class.java) }
        listOf(a, b).forEach { it.registerInitiatedFlow(GetSanctionsListFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `issues sanctions list with expected issuer`() {
        val flow = IssueSanctionsListFlow.Initiator()
        val future = a.startFlow(flow)
        network.runNetwork()

        assertEquals(a.info.legalIdentities.first(), future.get().state.data.issuer)
    }


    @Test
    fun `collect newest sanctions list`() {
        val issueFlow = IssueSanctionsListFlow.Initiator()
        val issueListFuture = a.startFlow(issueFlow)
        network.runNetwork()
        assertEquals(a.info.legalIdentities.first(), issueListFuture.get().state.data.issuer)

        val getFlow = GetSanctionsListFlow.Initiator(a.info.legalIdentities.first())
        val getListFuture = b.startFlow(getFlow)
        network.runNetwork()

        assertEquals(issueListFuture.get().state.data, getListFuture.get().single().state.data)

    }


    @Test
    fun `updates sanctions list with new sanctionee`() {
        val issueFlow = IssueSanctionsListFlow.Initiator()
        val issueListFuture = a.startFlow(issueFlow)
        network.runNetwork()
        assertEquals(a.info.legalIdentities.first(), issueListFuture.get().state.data.issuer)

        val getFlow = GetSanctionsListFlow.Initiator(a.info.legalIdentities.first())
        val getListFuture = b.startFlow(getFlow)
        network.runNetwork()
        assertEquals(issueListFuture.get().state.data, getListFuture.get().single().state.data)

        val updateFlow = UpdateSanctionsListFlow.Initiator(c.info.legalIdentities.first())
        val updateListFuture = a.startFlow(updateFlow)
        network.runNetwork()

        assertTrue {
            c.info.legalIdentities.first() in updateListFuture.get().state.data.badPeople
        }

        val getUpdatedListFlow = GetSanctionsListFlow.Initiator(a.info.legalIdentities.first())
        val getUpdatedListFuture = b.startFlow(getUpdatedListFlow)
        network.runNetwork()
        assertEquals(updateListFuture.get().state.data, getUpdatedListFuture.get().single().state.data)
        println(getUpdatedListFuture.get().single().state.data)
    }
}