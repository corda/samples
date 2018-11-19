package com.whistleblower

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to using deployNodes)
 * Do not use in a production environment.
 */
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf("ALL"))
    driver(DriverParameters(isDebug = true, startNodesInProcess = true, waitForAllNodesToFinish = true)) {
        val (nodeA, nodeB, nodeC, nodeD) = listOf(
                startNode(providedName = CordaX500Name("BraveEmployee", "Nairobi", "KE"), rpcUsers = listOf(user)).getOrThrow(),
                startNode(providedName = CordaX500Name("TradeBody", "Kisumu", "KE"), rpcUsers = listOf(user)).getOrThrow(),
                startNode(providedName = CordaX500Name("GovAgency", "Mombasa", "KE"), rpcUsers = listOf(user)).getOrThrow(),
                startNode(providedName = CordaX500Name("BadCompany", "Eldoret", "KE"), rpcUsers = listOf(user)).getOrThrow())

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
        startWebserver(nodeD)
    }
}
