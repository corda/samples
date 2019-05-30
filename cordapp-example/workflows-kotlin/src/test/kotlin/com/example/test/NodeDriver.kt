package com.example.test

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User

/**
 * This file is exclusively for being able to run your nodes through an IDE.
 * Do not use in a production environment.
 */
fun main(args: Array<String>) {
    val user = User("user1", "test", permissions = setOf("ALL"))
    driver(DriverParameters(waitForAllNodesToFinish = true, cordappsForAllNodes = listOf(TestCordapp.findCordapp("com.example.flow")))) {
        val nodeFutures = listOf(
                startNode(
                        providedName = CordaX500Name("PartyA", "London", "GB"),
                        rpcUsers = listOf(user)),
                startNode(
                        providedName = CordaX500Name("PartyB", "New York", "US"),
                        rpcUsers = listOf(user)),
                startNode(
                        providedName = CordaX500Name("PartyC", "Paris", "FR"),
                        rpcUsers = listOf(user)))

        nodeFutures.map { it.getOrThrow() }
    }
}
