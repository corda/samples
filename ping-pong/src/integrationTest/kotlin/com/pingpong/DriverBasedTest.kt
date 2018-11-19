package com.pingpong

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.junit.Test
import kotlin.test.assertFailsWith

class DriverBasedTest {
    @Test
    fun `run driver test`() {
        val bankA = TestIdentity(CordaX500Name("Bank A", "", "GB"))
        val bankB = TestIdentity(CordaX500Name("Bank B", "", "GB"))
        val bankC = TestIdentity(CordaX500Name("Bank C", "", "GB"))

        val user = User("user1", "test", permissions = setOf("ALL"))

        driver(DriverParameters().withStartNodesInProcess(true)) {
            val (_, nodeAHandle, _) = listOf(
                    startNode(providedName = bankA.name, rpcUsers = listOf(user)),
                    startNode(providedName = bankB.name, rpcUsers = listOf(user))
            ).map { it.getOrThrow() }

            val nodeARpcAddress = nodeAHandle.rpcAddress.toString()
            val nodeARpcClient = RpcClient(nodeARpcAddress)

            // We can ping Bank B...
            nodeARpcClient.ping(bankB.name.toString())
            // ...but not Bank C, who isn't on the network
            assertFailsWith<IllegalArgumentException> {
                nodeARpcClient.ping(bankC.name.toString())
            }
        }
    }
}