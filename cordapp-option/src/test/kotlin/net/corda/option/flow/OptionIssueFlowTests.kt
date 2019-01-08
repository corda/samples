package net.corda.option.flow

import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.option.base.KNOWN_SPOTS
import net.corda.option.base.KNOWN_VOLATILITIES
import net.corda.option.base.OPTION_CURRENCY
import net.corda.option.base.ORACLE_NAME
import net.corda.option.base.contract.OptionContract
import net.corda.option.base.state.OptionState
import net.corda.option.client.flow.OptionIssueFlow
import net.corda.option.createBadOption
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

class OptionIssueFlowTests {
    private val mockNet: MockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("net.corda.option.base.contract"),
            TestCordapp.findCordapp("net.corda.option.oracle.oracle"),
            TestCordapp.findCordapp("net.corda.finance"))))
    private val issuerNode = mockNet.createPartyNode()
    private val buyerNode = mockNet.createPartyNode()
    private val oracleNode = mockNet.createNode(legalName = ORACLE_NAME)

    private val issuer = issuerNode.info.legalIdentities.first()
    private val buyer = buyerNode.info.legalIdentities.first()
    private val oracle = oracleNode.info.legalIdentities.first()

    @Before
    fun setup() {
        oracleNode.registerInitiatedFlow(QueryOracleHandler::class.java)
        oracleNode.registerInitiatedFlow(RequestOracleSigHandler::class.java)

        listOf(issuerNode, buyerNode).forEach {
            it.registerInitiatedFlow(OptionIssueFlow.Responder::class.java)
        }

        mockNet.runNetwork()
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    @Test
    fun `issue flow records a correctly-formed transaction in both parties' transaction storages`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        val stx = issueOptionToBuyer(option)

        // We check the recorded transaction in both vaults.
        listOf(issuerNode, buyerNode).forEach { node ->
            assertEquals(stx, node.services.validatedTransactions.getTransaction(stx.id))

            val ltx = node.transaction {
                stx.toLedgerTransaction(node.services)
            }

            // A single Cash.State input.
            assertEquals(1, ltx.inputs.size)
            assertEquals(1, ltx.inputsOfType<Cash.State>().size)

            // A Cash.State output and an OptionState output.
            assertEquals(2, ltx.outputs.size)
            assertEquals(1, ltx.outputsOfType<Cash.State>().size)
            assertEquals(1, ltx.outputsOfType<OptionState>().size)

            // Two commands.
            assertEquals(3, ltx.commands.size)

            // An OptionContract.Commands.Issue command with the correct attributes.
            val optionCmd = ltx.commandsOfType<OptionContract.Commands.Issue>().single()
            assert(optionCmd.signers.containsAll(listOf(issuer.owningKey, buyer.owningKey)))

            // An OptionContract.OracleCommand with the correct attributes.
            val oracleCmd = ltx.commandsOfType<OptionContract.OracleCommand>().single()
            assert(oracleCmd.signers.contains(oracle.owningKey))
            assertEquals(KNOWN_SPOTS[0], oracleCmd.value.spotPrice)
            assertEquals(KNOWN_VOLATILITIES[0], oracleCmd.value.volatility)

            // A Cash.Commands.Move command with the correct attributes.
            val cashCmd = ltx.commandsOfType<Cash.Commands.Move>().single()
            assert(cashCmd.signers.contains(buyer.owningKey))
        }
    }

    @Test
    fun `flow records the option in the vaults of the issuer and owner`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        issueOptionToBuyer(option)

        // We check the recorded option in both vaults.
        listOf(issuerNode, buyerNode).forEach { node ->
            val options = node.transaction {
                node.services.vaultService.queryBy<OptionState>().states
            }
            assertEquals(1, options.size)
            val recordedOption = options.single().state.data
            assertEquals(option, recordedOption)
        }
    }

    @Test
    fun `flow records the correct cash in the issuer's vault`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        issueOptionToBuyer(option)

        // We check the recorded option in both vaults.
        val cash = issuerNode.transaction {
            issuerNode.services.vaultService.queryBy<Cash.State>().states
        }
        assertEquals(1, cash.size)
        val recordedCash = cash.single().state.data
        assertEquals(recordedCash.amount.quantity, 900)
        assertEquals(recordedCash.amount.token.product, OPTION_CURRENCY)
    }

    @Test
    fun `issue flow can only be run by the buyer`() {
        issueCashToBuyer()
        val option = createOption(issuer, buyer)
        val flow = OptionIssueFlow.Initiator(option)
        val future = issuerNode.startFlow(flow)
        mockNet.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    @Test
    fun `issue flow rejects options with an expiry date in the past`() {
        issueCashToBuyer()
        val badOption = createBadOption(issuer, buyer)
        val futureOne = buyerNode.startFlow(OptionIssueFlow.Initiator(badOption))
        mockNet.runNetwork()
        assertFailsWith<TransactionVerificationException> { futureOne.getOrThrow() }
    }

    @Test
    fun `issue flow fails if the buyer does not have enough cash`() {
        val option = createOption(issuer, buyer)
        val flow = OptionIssueFlow.Initiator(option)
        val future = buyerNode.startFlow(flow)
        mockNet.runNetwork()
        assertFailsWith<InsufficientBalanceException> { future.getOrThrow() }
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
}