package com.template

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User

fun main(args: Array<String>) {
    val nodeName = CordaX500Name("PartyA", "London", "GB")
    val rpcUser = User("user1", "test", permissions = setOf("ALL"))
    val driverParameters = DriverParameters(
            waitForAllNodesToFinish = true,
            notarySpecs = listOf())
    driver(driverParameters) {
        startNode(providedName = nodeName, rpcUsers = listOf(rpcUser)).getOrThrow()
    }
}