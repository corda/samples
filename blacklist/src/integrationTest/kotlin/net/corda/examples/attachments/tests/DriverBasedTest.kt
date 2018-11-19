package net.corda.examples.attachments.tests

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import org.junit.Assert
import org.junit.Test

class DriverBasedTest {
    private val bankAName = CordaX500Name("BankA", "", "GB")
    private val bankBName = CordaX500Name("BankB", "", "GB")

    @Test
    fun `run driver test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (nodeAHandle, nodeBHandle) = listOf(
                    startNode(providedName = bankAName),
                    startNode(providedName = bankBName)
            ).map { it.getOrThrow() }

            // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
            // started and can communicate. This is a very basic test, in practice tests would be starting flows,
            // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
            // as intended.
            Assert.assertEquals(nodeAHandle.rpc.wellKnownPartyFromX500Name(bankAName)!!.name, bankAName)
            Assert.assertEquals(nodeBHandle.rpc.wellKnownPartyFromX500Name(bankBName)!!.name, bankBName)
        }
    }
}