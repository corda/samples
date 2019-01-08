package net.corda.option

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes via
 * Gradle).
 *
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Run the "Run CorDapp - Kotlin" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports for each node, which should be output to the console. The "Debug CorDapp" configuration runs
 *    with port 5007, which should be "PartyA". In any case, double-check the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
    driver(DriverParameters(
            cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("net.corda.finance.contracts.asset")),
            isDebug = true,
            waitForAllNodesToFinish = true),
            dsl = {
                val user = User("user1", "test", permissions = setOf("ALL"))

                val (issuer, partyA, partyB, oracle) = listOf(
                        startNode(providedName = CordaX500Name("Issuer", "London", "GB"), rpcUsers = listOf(user)),
                        startNode(providedName = CordaX500Name("PartyA", "New York", "US"), rpcUsers = listOf(user)),
                        startNode(providedName = CordaX500Name("PartyB", "Paris", "FR"), rpcUsers = listOf(user)),
                        startNode(providedName = CordaX500Name("Oracle", "New York", "US"), rpcUsers = listOf(user))
                ).map { it.getOrThrow() }

                startWebserver(issuer)
                startWebserver(partyA)
                startWebserver(partyB)
                startWebserver(oracle)
            })
}