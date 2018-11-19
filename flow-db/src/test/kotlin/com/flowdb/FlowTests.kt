package com.flowdb

import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val BITCOIN = "bitcoin"
private val E_CASH = "eCash"
private val INITIAL_BITCOIN_VALUE = 7000
private val NEW_BITCOIN_VALUE = 8000
private val INITIAL_E_CASH_VALUE = 100
private val NEW_E_CASH_VALUE = 200

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.flowdb"))
        a = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flowWritesToTableCorrectly`() {
        val flow1 = AddTokenValueFlow(BITCOIN, INITIAL_BITCOIN_VALUE)
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        future1.get()

        val flow2 = QueryTokenValueFlow(BITCOIN)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val bitcoinValueFromDB = future2.get()

        assertEquals(INITIAL_BITCOIN_VALUE, bitcoinValueFromDB)
    }

    @Test
    fun `flowUpdatesTableCorrectly`() {
        val flow1 = AddTokenValueFlow(BITCOIN, INITIAL_BITCOIN_VALUE)
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        future1.get()

        val flow2 = UpdateTokenValueFlow(BITCOIN, NEW_BITCOIN_VALUE)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        future2.get()

        val flow3 = QueryTokenValueFlow(BITCOIN)
        val future3 = a.startFlow(flow3)
        network.runNetwork()
        val bitcoinValueFromDB = future3.get()

        assertEquals(NEW_BITCOIN_VALUE, bitcoinValueFromDB)
    }

    @Test
    fun `tableSupportsMultipleTokensCorrectly`() {
        val flow1 = AddTokenValueFlow(BITCOIN, INITIAL_BITCOIN_VALUE)
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        future1.get()

        val flow2 = UpdateTokenValueFlow(BITCOIN, NEW_BITCOIN_VALUE)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        future2.get()

        val flow3 = AddTokenValueFlow(E_CASH, INITIAL_E_CASH_VALUE)
        val future3 = a.startFlow(flow3)
        network.runNetwork()
        future3.get()

        val flow4 = UpdateTokenValueFlow(E_CASH, NEW_E_CASH_VALUE)
        val future4 = a.startFlow(flow4)
        network.runNetwork()
        future4.get()

        val flow5 = QueryTokenValueFlow(BITCOIN)
        val future5 = a.startFlow(flow5)
        network.runNetwork()
        val bitcoinValueFromDB = future5.get()

        val flow6 = QueryTokenValueFlow(E_CASH)
        val future6 = a.startFlow(flow6)
        network.runNetwork()
        val eCashValueFromDB = future6.get()

        assertEquals(NEW_BITCOIN_VALUE, bitcoinValueFromDB)
        assertEquals(NEW_E_CASH_VALUE, eCashValueFromDB)
    }

    @Test
    fun `errorIsThrownIfTokenNotInTable`() {
        val flow = QueryTokenValueFlow(BITCOIN)
        val future = a.startFlow(flow)
        network.runNetwork()

        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }
}