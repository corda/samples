package net.corda.examples.yo

import net.corda.examples.yo.flows.YoFlow
import net.corda.examples.yo.flows.YoFlowResponder
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("net.corda.examples.yo.contracts"),
        TestCordapp.findCordapp("net.corda.examples.yo.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(YoFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    //The yo flow should not have any input
    //This test will check if the input list is empty
    @Test
    fun `dummy test`() {
        val future = a.startFlow(YoFlow(b.info.legalIdentities.first()))
        network.runNetwork()
        val ptx = future.get()
        assert(ptx.tx.inputs.isEmpty())
    }
}