package net.corda.examples.obligation.workflow

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User

fun main(args: Array<String>) {
    val user = User("user1", "test", permissions = setOf("ALL"))

    driver(DriverParameters(
            cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("net.corda.finance.contracts.asset"),
                    TestCordapp.findCordapp("net.corda.finance.schemas")),
            isDebug = true,
            startNodesInProcess = true,
            waitForAllNodesToFinish = true)) {
        val (nodeA, nodeB, nodeC) = listOf(
                startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("PartyB", "New York", "US"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("PartyC", "Paris", "FR"), rpcUsers = listOf(user))
        ).map { it.getOrThrow() }

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
    }
}