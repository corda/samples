package com.flowdb

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
    val user = User("user1", "test", permissions = setOf("StartFlow.com.flowhttp.HttpCallFlow"))
    driver(DriverParameters().withIsDebug(true).withWaitForAllNodesToFinish(true)) {
        startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
    }
}