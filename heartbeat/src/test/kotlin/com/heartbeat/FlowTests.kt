package com.heartbeat

import net.corda.client.rpc.notUsed
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    lateinit var network: MockNetwork
    lateinit var node: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.heartbeat"), threadPerNode = true)
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

        val enoughTimeForFiveScheduledTxs: Long = 5500
        Thread.sleep(enoughTimeForFiveScheduledTxs)

        val recordedTxs = node.transaction {
            val (recordedTxs, futureTxs) = node.services.validatedTransactions.track()
            futureTxs.notUsed()
            recordedTxs
        }

        val originalTxPlusFiveScheduledTxs = 6
        assertEquals(originalTxPlusFiveScheduledTxs, recordedTxs.size)
    }
}