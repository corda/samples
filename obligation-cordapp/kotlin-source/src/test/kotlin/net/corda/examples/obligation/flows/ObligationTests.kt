package net.corda.examples.obligation.flows

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import java.util.*

/**
 * A base class to reduce the boilerplate when writing obligation flow tests.
 */
abstract class ObligationTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("net.corda.examples.obligation", "net.corda.finance", "net.corda.finance.schemas"), threadPerNode = true)

        a = network.createNode()
        b = network.createNode()
        c = network.createNode()
        val nodes = listOf(a, b, c)

        nodes.forEach {
            it.registerInitiatedFlow(IssueObligation.Responder::class.java)
            it.registerInitiatedFlow(TransferObligation.Responder::class.java)
        }
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    protected fun issueObligation(borrower: StartedMockNode,
                                  lender: StartedMockNode,
                                  amount: Amount<Currency>,
                                  anonymous: Boolean = true
    ): net.corda.core.transactions.SignedTransaction {
        val lenderIdentity = lender.info.chooseIdentity()
        val flow = IssueObligation.Initiator(amount, lenderIdentity, anonymous)
        return borrower.startFlow(flow).getOrThrow()
    }

    protected fun transferObligation(linearId: UniqueIdentifier,
                                     lender: StartedMockNode,
                                     newLender: StartedMockNode,
                                     anonymous: Boolean = true
    ): net.corda.core.transactions.SignedTransaction {
        val newLenderIdentity = newLender.info.chooseIdentity()
        val flow = TransferObligation.Initiator(linearId, newLenderIdentity, anonymous)
        return lender.startFlow(flow).getOrThrow()
    }

    protected fun settleObligation(linearId: UniqueIdentifier,
                                   borrower: StartedMockNode,
                                   amount: Amount<Currency>,
                                   anonymous: Boolean = true
    ): net.corda.core.transactions.SignedTransaction {
        val flow = SettleObligation.Initiator(linearId, amount, anonymous)
        return borrower.startFlow(flow).getOrThrow()
    }

    protected fun selfIssueCash(party: StartedMockNode,
                                amount: Amount<Currency>): net.corda.core.transactions.SignedTransaction {
        val notary = party.services.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw IllegalStateException("Could not find a notary.")
        val issueRef = OpaqueBytes.of(0)
        val issueRequest = CashIssueFlow.IssueRequest(amount, issueRef, notary)
        val flow = CashIssueFlow(issueRequest)
        return party.startFlow(flow).getOrThrow().stx
    }
}
