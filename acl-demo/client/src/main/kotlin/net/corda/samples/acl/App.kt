package net.corda.samples.acl

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.openHttpConnection
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@InitiatingFlow
@StartableByRPC
class PingFlow(val target: Party) : FlowLogic<Unit>() {
    override val progressTracker: ProgressTracker = ProgressTracker()
    @Suspendable
    override fun call() {
        // We only check that the target is on the whitelist.
        // We don't check whether _WE_ are on the whitelist!
        val acl = serviceHub.cordaService(AclService::class.java).list
        if (target.name !in acl) {
            throw FlowException("$target is not on the whitelist.")
        }

        val session = initiateFlow(target)
        println("Sending PING to $target")
        val response = session.sendAndReceive<String>("PING").unwrap { it }
        println("Received $response from $target")
    }
}

@InitiatedBy(PingFlow::class)
class PongFlow(val otherSession: FlowSession) : FlowLogic<Unit>() {
    override val progressTracker: ProgressTracker = ProgressTracker()
    @Suspendable
    override fun call() {
        // In case a "pinging" node is not on the whitelist.
        val acl = serviceHub.cordaService(AclService::class.java).list
        if (otherSession.counterparty.name !in acl) {
            throw FlowException("${otherSession.counterparty} is not on the whitelist.")
        }

        val response = otherSession.receive<String>()
        println("Received $response from ${otherSession.counterparty}")

        println("Sending Pong to ${otherSession.counterparty}!")
        otherSession.send("PONG")
    }
}

@CordaService
class AclService(val services: AppServiceHub) : SingletonSerializeAsToken() {
    @Volatile
    var _list: Set<CordaX500Name> = getAcl()
        private set
    val list get() = _list

    private val task = object : TimerTask() {
        override fun run() {
            // Only update if the set of CordaX500Names is not empty.
            val list = getAcl()
            if (list.isNotEmpty()) {
                _list = list
            }
        }
    }

    // Poll the ACL every second. Unnecessary but makes the demo responsive.
    init {
        Timer().schedule(task, Date(), 1000)
    }

    private fun getAcl(): Set<CordaX500Name> {
        val conn = URL("http://localhost:8000/acl").openHttpConnection()
        conn.requestMethod = "GET"
        return when (conn.responseCode) {
            HttpURLConnection.HTTP_OK -> conn.inputStream.reader().readLines().map { CordaX500Name.parse(it) }.toSet()
            HttpURLConnection.HTTP_INTERNAL_ERROR -> emptySet()
            else -> throw IOException("Response Code ${conn.responseCode}: ${conn.responseMessage}")
        }
    }
}