package net.corda.examples.sendfile

import net.corda.examples.sendfile.flows.SendAttachment
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("net.corda.examples.sendfile.contracts"),
        TestCordapp.findCordapp("net.corda.examples.sendfile.flows")
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

    //Test #1 check attachments list has more than one element
    //one for contract attachment, another one for attached zip
    //Make sure change the file path in the SendAttachment flow to "../test.zip" for passing the unit test.
    //because the unit test are in a different working directory than the running node.
    @Test
    fun `dummy test`() {
        val future = a.startFlow(SendAttachment(b.info.legalIdentities.first()))
        network.runNetwork()
        val ptx = future.get()
        assert(ptx.tx.attachments.size > 1)
    }
}

