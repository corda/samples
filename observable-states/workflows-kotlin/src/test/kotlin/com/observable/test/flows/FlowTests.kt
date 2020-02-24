package com.observable.test.flows

import com.observable.flows.ReportManuallyResponder
import com.observable.flows.TradeAndReport
import com.observable.flows.TradeAndReportResponder
import com.observable.states.HighlyRegulatedState
import net.corda.core.node.services.queryBy
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.observable.contracts"),
            TestCordapp.findCordapp("com.observable.flows")
    )))
    private val seller = network.createNode()
    private val buyer = network.createNode()
    private val stateRegulator = network.createNode()
    private val nationalRegulator = network.createNode()

    init {
        listOf(buyer, stateRegulator).forEach {
            it.registerInitiatedFlow(TradeAndReportResponder::class.java)
        }
        nationalRegulator.registerInitiatedFlow(ReportManuallyResponder::class.java)
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `everyone records the trade including the regulators`() {
        val flow = TradeAndReport(
                buyer.info.singleIdentity(),
                stateRegulator.info.singleIdentity(),
                nationalRegulator.info.singleIdentity())
        val future = seller.startFlow(flow)
        network.runNetwork()
        future.get()

        listOf(seller, buyer, stateRegulator, nationalRegulator).forEach { node ->
            node.transaction {
                val highlyRegulatedStates = node.services.vaultService.queryBy<HighlyRegulatedState>().states
                assertEquals(1, highlyRegulatedStates.size)
            }
        }
    }
}