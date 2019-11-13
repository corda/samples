package com.attachZIP

import com.attachZIP.flows.sendAttachment
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.attachZIP.contracts"),
        TestCordapp.findCordapp("com.attachZIP.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `dummy test`() {


        val future = a.startFlow(sendAttachment(b.info.legalIdentities.first()))
        network.runNetwork()
        val tx = future.get()
    }
}

