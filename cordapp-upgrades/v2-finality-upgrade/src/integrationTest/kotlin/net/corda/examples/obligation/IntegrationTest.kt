package net.corda.examples.obligation

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import org.junit.Assert
import org.junit.Test

class IntegrationTest {
    private val nodeAName = CordaX500Name("NodeA", "", "GB")
    private val nodeBName = CordaX500Name("NodeB", "", "US")

    @Test
    fun `run driver test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (nodeAHandle, nodeBHandle) = listOf(
                    startNode(providedName = nodeAName),
                    startNode(providedName = nodeBName)
            ).map { it.getOrThrow() }

            // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
            // started and can communicate. This is a very basic test, in practice tests would be starting flows,
            // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
            // as intended.
            Assert.assertEquals(nodeAHandle.rpc.wellKnownPartyFromX500Name(nodeBName)!!.name, nodeBName)
            Assert.assertEquals(nodeBHandle.rpc.wellKnownPartyFromX500Name(nodeAName)!!.name, nodeAName)
        }
    }
}
