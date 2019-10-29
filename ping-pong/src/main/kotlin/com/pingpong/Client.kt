package com.pingpong

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor

val RPC_USERNAME = "user1"
val RPC_PASSWORD = "test"

fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: RpcClient <node address> <counterpartyName>" }
    println(args[0])
    println(args[1])
    val rpcAddressString = args[0]
    val counterpartyName = args[1]

    val rpcClient = RpcClient(rpcAddressString)
    rpcClient.ping(counterpartyName)
    rpcClient.closeRpcConnection()
}

/** An example RPC client that connects to a node and performs various example operations. */
class RpcClient(rpcAddressString: String) {
    companion object {
        val logger = loggerFor<RpcClient>()
    }

    private val rpcConnection: CordaRPCConnection

    init {
        rpcConnection = establishRpcConnection(rpcAddressString, RPC_USERNAME, RPC_PASSWORD)
    }

    /** Returns a [CordaRPCConnection] to the node listening on [rpcPortString]. */
    private fun establishRpcConnection(rpcAddressString: String, username: String, password: String): CordaRPCConnection {
        val nodeAddress = parse(rpcAddressString)
        val client = CordaRPCClient(nodeAddress)
        return client.start(username, password)
    }

    /** Closes the [CordaRPCConnection]. */
    fun closeRpcConnection() {
        rpcConnection.close()
    }

    fun ping(counterpartyName: String) {
        val rpcProxy = rpcConnection.proxy

        pingCounterparty(rpcProxy, counterpartyName)

        // println prints to IntelliJ console.
        // TODO: Make logger output show in IntelliJ console.
        println("Successfully pinged $counterpartyName.")
        logger.info("Successfully pinged $counterpartyName.")
    }

    /** Pings the counterparty. */
    private fun pingCounterparty(rpcProxy: CordaRPCOps, counterpartyName: String) {
        val counterpartyX500Name = CordaX500Name.parse(counterpartyName)
        val counterparty = rpcProxy.wellKnownPartyFromX500Name(counterpartyX500Name)
                ?: throw IllegalArgumentException("Peer $counterpartyName not found in the network map.")

        val flowFuture = rpcProxy.startFlow(::Ping, counterparty).returnValue
        flowFuture.getOrThrow()
    }
}
