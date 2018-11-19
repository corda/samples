package com.flowhttp

import co.paralleluniverse.fibers.Suspendable
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import okhttp3.OkHttpClient
import okhttp3.Request

val BITCOIN_README_URL = "https://raw.githubusercontent.com/bitcoin/bitcoin/4405b78d6059e536c36974088a8ed4d9f0f29898/readme.txt"

@InitiatingFlow
@StartableByRPC
class HttpCallFlow : FlowLogic<String>() {
    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {
        val httpRequest = Request.Builder().url(BITCOIN_README_URL).build()

        // BE CAREFUL when making HTTP calls in flows:
        // 1. The request must be executed in a BLOCKING way. Flows don't
        //    currently support suspending to await an HTTP call's response
        // 2. The request must be idempotent. If the flow fails and has to
        //    restart from a checkpoint, the request will also be replayed
        val httpResponse = OkHttpClient().newCall(httpRequest).execute()

        return httpResponse.body().string()
    }
}

class Client {
    companion object {
        val logger = loggerFor<Client>()
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: Client <node address>" }
        val nodeAddress = parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the build.gradle file.
        val proxy = client.start("user1", "test").proxy

        // Run the HttpCallFlow and retrieve its response value.
        val returnValue = proxy.startFlow(::HttpCallFlow).returnValue.get()

        logger.info(returnValue)
    }
}

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream the contents of the node's vault.
 */
fun main(args: Array<String>) {
    Client().main(args)
}