package com.heartbeat

import net.corda.client.rpc.notUsed
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var node: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(threadPerNode = true, cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.heartbeat"))))
        node = network.createNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `heartbeat occurs every second`() {
        val flow = StartHeartbeatFlow()
        node.startFlow(flow).get()

        val sleepTime: Long = 6000
        Thread.sleep(sleepTime)

        val recordedTxs = node.transaction {
            val (recordedTxs, futureTxs) = node.services.validatedTransactions.track()
            futureTxs.notUsed()
            recordedTxs
        }

        val totalExpectedTransactions = 7
        assertEquals(totalExpectedTransactions, recordedTxs.size)
    }
}