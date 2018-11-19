package com.negotiation.flows

import com.negotiation.ModificationFlow
import com.negotiation.ProposalFlow
import com.negotiation.ProposalState
import net.corda.core.flows.FlowException
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import org.junit.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModificationFlowTests: FlowTestsBase() {

    @Test
    fun `modification flow consumes the proposals in both nodes' vaults and replaces them with equivalent proposals but with new amounts when initiator is buyer`() {
        testModificationForRole(ProposalFlow.Role.Seller)
    }

    @Test
    fun `modification flow consumes the proposals in both nodes' vaults and replaces them with equivalent proposals but with new amounts when initiator is seller`() {
        testModificationForRole(ProposalFlow.Role.Buyer)
    }

    @Test
    fun `modification flow throws an error is the proposer tries to modify the proposal`() {
        val oldAmount = 1
        val newAmount = 2
        val counterparty = b.info.chooseIdentity()
        val proposalId = nodeACreatesProposal(ProposalFlow.Role.Buyer, oldAmount, counterparty)

        val flow = ModificationFlow.Initiator(proposalId, newAmount)
        val future = a.startFlow(flow)
        network.runNetwork()
        val exceptionFromFlow = assertFailsWith<ExecutionException> {
            future.get()
        }.cause!!
        assertEquals(FlowException::class, exceptionFromFlow::class)
        assertEquals("Only the proposee can modify a proposal.", exceptionFromFlow.message)
    }

    private fun testModificationForRole(role: ProposalFlow.Role) {
        val oldAmount = 1
        val newAmount = 2
        val counterparty = b.info.chooseIdentity()

        val proposalId = nodeACreatesProposal(role, oldAmount, counterparty)
        nodeBModifiesProposal(proposalId, newAmount)

        for (node in listOf(a, b)) {
            node.transaction {
                val proposals = node.services.vaultService.queryBy<ProposalState>().states
                assertEquals(1, proposals.size)
                val proposal = proposals.single().state.data

                assertEquals(newAmount, proposal.amount)
                val (buyer, proposer, seller, proposee) = when (role) {
                    ProposalFlow.Role.Buyer -> listOf(a.info.chooseIdentity(), b.info.chooseIdentity(), b.info.chooseIdentity(), a.info.chooseIdentity())
                    ProposalFlow.Role.Seller -> listOf(b.info.chooseIdentity(), b.info.chooseIdentity(), a.info.chooseIdentity(), a.info.chooseIdentity())
                }

                assertEquals(buyer, proposal.buyer)
                assertEquals(proposer, proposal.proposer)
                assertEquals(seller, proposal.seller)
                assertEquals(proposee, proposal.proposee)
            }
        }
    }
}