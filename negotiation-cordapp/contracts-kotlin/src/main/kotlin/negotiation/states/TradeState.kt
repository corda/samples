package negotiation.states

import negotiation.contracts.ProposalAndTradeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


@BelongsToContract(ProposalAndTradeContract::class)
data class TradeState(
        val amount: Int,
        val buyer: Party,
        val seller: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants = listOf(buyer, seller)
}