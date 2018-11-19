package com.negotiation.flows

import com.negotiation.AcceptanceFlow
import com.negotiation.ProposalFlow
import com.negotiation.ProposalState
import com.negotiation.TradeState
import net.corda.core.flows.FlowException
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import org.junit.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AcceptanceFlowTests: FlowTestsBase() {
    @Test
    fun `acceptance flow consumes the proposals in both nodes' vaults and replaces them with equivalent accepted trades when initiator is buyer`() {
        testAcceptanceForRole(ProposalFlow.Role.Seller)
    }

    @Test
    fun `acceptance flow consumes the proposals in both nodes' vaults and replaces them with equivalent accepted trades when initiator is seller`() {
        testAcceptanceForRole(ProposalFlow.Role.Buyer)
    }

    @Test
    fun `acceptance flow throws an error is the proposer tries to accept the proposal`() {
        val amount = 1
        val counterparty = b.info.chooseIdentity()
        val proposalId = nodeACreatesProposal(ProposalFlow.Role.Buyer, amount, counterparty)

        val flow = AcceptanceFlow.Initiator(proposalId)
        val future = a.startFlow(flow)
        network.runNetwork()
        val exceptionFromFlow = assertFailsWith<ExecutionException> {
            future.get()
        }.cause!!
        assertEquals(FlowException::class, exceptionFromFlow::class)
        assertEquals("Only the proposee can accept a proposal.", exceptionFromFlow.message)
    }

    private fun testAcceptanceForRole(role: ProposalFlow.Role) {
        val amount = 1
        val counterparty = b.info.chooseIdentity()

        val proposalId = nodeACreatesProposal(role, amount, counterparty)
        nodeBAcceptsProposal(proposalId)

        for (node in listOf(a, b)) {
            node.transaction {
                val proposals = node.services.vaultService.queryBy<ProposalState>().states
                assertEquals(0, proposals.size)

                val trades = node.services.vaultService.queryBy<TradeState>().states
                assertEquals(1, trades.size)
                val trade = trades.single().state.data

                assertEquals(amount, trade.amount)
                val (buyer, seller) = when (role) {
                    ProposalFlow.Role.Buyer -> listOf(a.info.chooseIdentity(), b.info.chooseIdentity())
                    ProposalFlow.Role.Seller -> listOf(b.info.chooseIdentity(), a.info.chooseIdentity())
                }

                assertEquals(buyer, trade.buyer)
                assertEquals(seller, trade.seller)
            }
        }
    }
}