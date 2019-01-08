package net.corda.option.flow

import net.corda.core.contracts.Amount
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.flows.CashIssueFlow
import net.corda.option.DUMMY_LINEAR_ID
import net.corda.option.base.OPTION_CURRENCY
import net.corda.option.base.ORACLE_NAME
import net.corda.option.base.contract.OptionContract
import net.corda.option.base.state.OptionState
import net.corda.option.client.flow.OptionExerciseFlow
import net.corda.option.client.flow.OptionIssueFlow
import net.corda.option.client.flow.OptionTradeFlow
import net.corda.option.createOption
import net.corda.option.oracle.flow.QueryOracleHandler
import net.corda.option.oracle.flow.RequestOracleSigHandler
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OptionExerciseFlowTests {
    private val mockNet: MockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("net.corda.option.base.contract"),
            TestCordapp.findCordapp("net.corda.option.oracle.oracle"),
            TestCordapp.findCordapp("net.corda.finance"))))
    private val issuerNode = mockNet.createPartyNode()
    private val buyerNode = mockNet.createPartyNode()
    private val oracleNode = mockNet.createNode(legalName = ORACLE_NAME)

    private val issuer = issuerNode.info.legalIdentities.first()
    private val buyer = buyerNode.info.legalIdentities.first()

    @Before
    fun setup() {
        oracleNode.registerInitiatedFlow(QueryOracleHandler::class.java)
        oracleNode.registerInitiatedFlow(RequestOracleSigHandler::class.java)

        listOf(issuerNode, buyerNode).forEach {
            it.registerInitiatedFlow(OptionIssueFlow.Responder::class.java)
            it.registerInitiatedFlow(OptionTradeFlow.Responder::class.java)
            it.registerInitiatedFlow(OptionExerciseFlow.Responder::class.java)
        }

        mockNet.runNetwork()
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    @Test
    fun `issue flow records a correctly-formed transaction in both parties transaction storages`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        issueOptionToBuyer(option)
        val stx = exerciseOption()

        // We check the recorded transaction in both vaults.
        listOf(issuerNode, buyerNode).forEach { node ->
            assertEquals(stx, node.services.validatedTransactions.getTransaction(stx.id))

            val ltx = node.transaction {
                stx.toLedgerTransaction(node.services)
            }

            // An OptionState input.
            assertEquals(1, ltx.inputs.size)
            assertEquals(1, ltx.inputsOfType<OptionState>().size)

            // An OptionState output.
            assertEquals(1, ltx.outputs.size)
            assertEquals(1, ltx.outputsOfType<OptionState>().size)

            // A single OptionContract.Commands.Exercise command with the correct attributes.
            assertEquals(1, ltx.commands.size)
            val optionCmd = ltx.commandsOfType<OptionContract.Commands.Exercise>().single()
            assert(optionCmd.signers.containsAll(listOf(buyer.owningKey)))
        }
    }

    @Test
    fun `flow records the option in the vaults of the issuer and owner`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        issueOptionToBuyer(option)
        exerciseOption()

        // We check the recorded option in both vaults.
        listOf(issuerNode, buyerNode).forEach { node ->
            val options = node.transaction {
                node.services.vaultService.queryBy<OptionState>().states
            }
            assertEquals(1, options.size)
            val recordedOption = options.single().state.data
            assertEquals(option, recordedOption.copy(exercised = false, exercisedOnDate = option.exercisedOnDate))
        }
    }

    @Test
    fun `exercise flow can only be run by the current owner`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        issueOptionToBuyer(option)
        val flow = OptionExerciseFlow.Initiator(DUMMY_LINEAR_ID)
        // We are running the flow from the issuer, who doesn't currently own the option.
        val future = issuerNode.startFlow(flow)
        mockNet.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    private fun issueCashToBuyer() {
        val notary = buyerNode.services.networkMapCache.notaryIdentities.first()
        val flow = CashIssueFlow(Amount(900, OPTION_CURRENCY), OpaqueBytes.of(0x01), notary)
        val future = buyerNode.startFlow(flow)
        mockNet.runNetwork()
        future.getOrThrow()
    }

    private fun issueOptionToBuyer(option: OptionState): SignedTransaction {
        val flow = OptionIssueFlow.Initiator(option)
        val future = buyerNode.startFlow(flow)
        mockNet.runNetwork()
        return future.getOrThrow()
    }

    private fun exerciseOption(): SignedTransaction {
        val flow = OptionExerciseFlow.Initiator(DUMMY_LINEAR_ID)
        val future = buyerNode.startFlow(flow)
        mockNet.runNetwork()
        return future.getOrThrow()
    }
}
