package com.example.test.flow

import com.example.flow.IssueInvoiceFlow
import com.example.flow.PayInvoiceFlow
import com.example.state.InvoiceState
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.BOC_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.internal.FINANCE_CORDAPPS
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PayInvoiceFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var contractor: StartedMockNode
    private lateinit var megaCorp: StartedMockNode
    private lateinit var bankOfCordaNode: StartedMockNode
    private lateinit var oracle: StartedMockNode
    private lateinit var today: LocalDate
    private val dummyRef = OpaqueBytes.of(0x01)

    @Before
    fun setup() {
        val corDapps = listOf(
                TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow")) +
                FINANCE_CORDAPPS

        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = corDapps))
        val oracleName = CordaX500Name("Oracle", "London","GB")
        contractor = network.createPartyNode()
        megaCorp = network.createPartyNode()
        bankOfCordaNode = network.createPartyNode(BOC_NAME)
        oracle = network.createNode(oracleName)

        // Inject some cash into MegaCorp so it can pay the contractor
        //bankOfCordaNode.startFlow(CashIssueAndPaymentFlow(1000.POUNDS, dummyRef, megaCorp.info.singleIdentity(), false, network.defaultNotaryIdentity))

        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(contractor, megaCorp).forEach { it.registerInitiatedFlow(IssueInvoiceFlow.Acceptor::class.java) }
        network.runNetwork()
        today = LocalDate.now()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    private fun submitInvoice(hoursWorked: Int): InvoiceState {
        val future = contractor.startFlow(IssueInvoiceFlow.Initiator(hoursWorked, today, megaCorp.info.singleIdentity()))
        network.runNetwork()
        return future.getOrThrow().tx.outputStates.first() as InvoiceState
    }

    @Ignore("If we're rejecting invalid invoices on issue then do we check for an invalid one here?")
    @Test
    fun `flow rejects invalid invoices`() {
        val invoice = submitInvoice(15) //InvoiceState(today, 15, 10.0, contractor.info.singleIdentity(), megaCorp.info.singleIdentity(), oracle.info.singleIdentity())
        val flow = PayInvoiceFlow.Initiator(invoice.linearId.id)
        val future = megaCorp.startFlow(flow)
        network.runNetwork()

        // The InvoiceContract specifies that hoursWorked cannot have negative values.
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is only signed by the initiator`() {
        val invoice = submitInvoice(8)
        //val invoice = InvoiceState(today, 8, 10.0, contractor.info.singleIdentity(), megaCorp.info.singleIdentity(), oracle.info.singleIdentity())
        val flow = PayInvoiceFlow.Initiator(invoice.linearId.id)
        val future = megaCorp.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(contractor.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in both parties' transaction storage`() {
        val invoice = submitInvoice(8)
        val flow = PayInvoiceFlow.Initiator(invoice.linearId.id)
        val future = megaCorp.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both parties transaction storage.
        assertEquals(signedTx, megaCorp.services.validatedTransactions.getTransaction(signedTx.id))
        assertEquals(signedTx, contractor.services.validatedTransactions.getTransaction(signedTx.id))
    }

    @Test
    fun `recorded transaction has the correct values for the payments`() {
        val hours = 8
        val submittedInvoice = submitInvoice(hours)
        val flow = PayInvoiceFlow.Initiator(submittedInvoice.linearId.id)
        val future = megaCorp.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(contractor, megaCorp)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)!!

            val recordedInvoice = recordedTx.tx.outputsOfType<InvoiceState>().single()
            assertEquals(recordedInvoice.hoursWorked, hours)
            assertEquals(recordedInvoice.contractor, contractor.info.singleIdentity())
            assertEquals(recordedInvoice.company, megaCorp.info.singleIdentity())

            // TODO: Check the payment values?
        }
    }
}