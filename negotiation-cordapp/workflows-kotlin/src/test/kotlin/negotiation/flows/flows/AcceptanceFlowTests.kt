package negotiation.flows.flows

import negotiation.flows.AcceptanceFlow
import negotiation.states.ProposalState
import negotiation.states.TradeState
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
        testAcceptance(false)
    }

    @Test
    fun `acceptance flow consumes the proposals in both nodes' vaults and replaces them with equivalent accepted trades when initiator is seller`() {
        testAcceptance(true)
    }

    @Test
    fun `acceptance flow throws an error is the proposer tries to accept the proposal`() {
        val amount = 1
        val counterparty = b.info.chooseIdentity()
        val proposalId = nodeACreatesProposal(true, amount, counterparty)

        val flow = AcceptanceFlow.Initiator(proposalId)
        val future = a.startFlow(flow)
        network.runNetwork()
        val exceptionFromFlow = assertFailsWith<ExecutionException> {
            future.get()
        }.cause!!
        assertEquals(FlowException::class, exceptionFromFlow::class)
        assertEquals("Only the proposee can accept a proposal.", exceptionFromFlow.message)
    }

    private fun testAcceptance(isBuyer: Boolean) {
        val amount = 1
        val counterparty = b.info.chooseIdentity()

        val proposalId = nodeACreatesProposal(isBuyer, amount, counterparty)
        nodeBAcceptsProposal(proposalId)

        for (node in listOf(a, b)) {
            node.transaction {
                val proposals = node.services.vaultService.queryBy<ProposalState>().states
                assertEquals(0, proposals.size)

                val trades = node.services.vaultService.queryBy<TradeState>().states
                assertEquals(1, trades.size)
                val trade = trades.single().state.data

                assertEquals(amount, trade.amount)
                val (buyer, seller) = when {
                    isBuyer -> listOf(a.info.chooseIdentity(), b.info.chooseIdentity())
                    else -> listOf(b.info.chooseIdentity(), a.info.chooseIdentity())
                }

                assertEquals(buyer, trade.buyer)
                assertEquals(seller, trade.seller)
            }
        }
    }
}