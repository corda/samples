package com.negotiation.flows

import com.negotiation.AcceptanceFlow
import com.negotiation.ModificationFlow
import com.negotiation.ProposalFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before

abstract class FlowTestsBase {
    protected lateinit var network: MockNetwork
    protected lateinit var a: StartedMockNode
    protected lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.negotiation"))
        a = network.createPartyNode()
        b = network.createPartyNode()

        val responseFlows = listOf(ProposalFlow.Responder::class.java, AcceptanceFlow.Responder::class.java, ModificationFlow.Responder::class.java)
        listOf(a, b).forEach {
            for (flow in responseFlows) {
                it.registerInitiatedFlow(flow)
            }
        }

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    fun nodeACreatesProposal(role: ProposalFlow.Role, amount: Int, counterparty: Party): UniqueIdentifier {
        val flow = ProposalFlow.Initiator(role, amount, counterparty)
        val future = a.startFlow(flow)
        network.runNetwork()
        return future.get()
    }

    fun nodeBAcceptsProposal(proposalId: UniqueIdentifier) {
        val flow = AcceptanceFlow.Initiator(proposalId)
        val future = b.startFlow(flow)
        network.runNetwork()
        future.get()

    }

    fun nodeBModifiesProposal(proposalId: UniqueIdentifier, newAmount: Int) {
        val flow = ModificationFlow.Initiator(proposalId, newAmount)
        val future = b.startFlow(flow)
        network.runNetwork()
        future.get()
    }
}