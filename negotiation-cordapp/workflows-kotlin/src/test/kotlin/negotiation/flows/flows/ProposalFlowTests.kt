package negotiation.flows.flows

import negotiation.states.ProposalState
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import org.junit.Test
import kotlin.test.assertEquals

class ProposalFlowTests: FlowTestsBase() {

    @Test
    fun `proposal flow creates the correct proposals in both nodes' vaults when initiator is buyer`() {
        testProposal(true)
    }

    @Test
    fun `proposal flow creates the correct proposals in both nodes' vaults when initiator is seller`() {
        testProposal(false)
    }

    private fun testProposal(isBuyer: Boolean) {
        val amount = 1
        val counterparty = b.info.chooseIdentity()

        nodeACreatesProposal(isBuyer, amount, counterparty)

        for (node in listOf(a, b)) {
            node.transaction {
                val proposals = node.services.vaultService.queryBy<ProposalState>().states
                assertEquals(1, proposals.size)
                val proposal = proposals.single().state.data

                assertEquals(amount, proposal.amount)
                val (buyer, proposer, seller, proposee) = when {
                    isBuyer -> listOf(a.info.chooseIdentity(), a.info.chooseIdentity(), b.info.chooseIdentity(), b.info.chooseIdentity())
                    else -> listOf(b.info.chooseIdentity(), a.info.chooseIdentity(), a.info.chooseIdentity(), b.info.chooseIdentity())
                }

                assertEquals(buyer, proposal.buyer)
                assertEquals(proposer, proposal.proposer)
                assertEquals(seller, proposal.seller)
                assertEquals(proposee, proposal.proposee)
            }
        }
    }
}