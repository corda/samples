package com.gitcoins

import com.gitcoins.webserver.GitCoinsWebServer
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.seconds
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.User
import org.junit.Test
import spring.springDriver
import java.time.Duration
import java.time.LocalDate

class ServerTest {

    companion object {
        private val log = contextLogger()
    }

    private val rpcUsers = listOf(User("user", "password", setOf("ALL")))
    private val currentDate: LocalDate = LocalDate.now()
    private val futureDate: LocalDate = currentDate.plusMonths(6)
    private val maxWaitTime: Duration = 60.seconds

    fun `run server test`() {
        springDriver(DriverParameters(
                useTestClock = true,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, rpcUsers = rpcUsers)),
                extraCordappPackagesToScan = listOf("com.gitcoins")
        )) {
            val (controller, nodeA, nodeB) = listOf(
                    defaultNotaryNode,
                    startNode(providedName = ALICE_NAME, rpcUsers = rpcUsers),
                    startNode(providedName = CordaX500Name("Regulator", "Moscow", "RU"))
            ).map { it.getOrThrow() }

            log.info("All nodes started")

            val (controllerAddr, nodeAAddr, nodeBAddr) = listOf(controller, nodeA, nodeB).map {
                startSpringBootWebapp(GitCoinsWebServer::class.java, it, "/api/git/")
            }.map { it.getOrThrow().listenAddress }
        }
    }
}