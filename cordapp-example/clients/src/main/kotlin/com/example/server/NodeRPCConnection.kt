package com.example.server

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Wraps a Corda node with a RPC proxy.
 *
 * The RPC proxy is configured based on the properties in `application.properties`.
 *
 * @param host The host of the node we are connecting to.
 * @param rpcPort The RPC port of the node we are connecting to.
 * @param username The username for logging into the RPC client.
 * @param password The password for logging into the RPC client.
 * @property proxy The RPC proxy.
 */


@Component
open class NodeRPCConnection : AutoCloseable {
    // The host of the node we are connecting to.
    @Value("\${config.rpc.host}")
    private val host: String? = null
    // The RPC port of the node we are connecting to.
    @Value("\${config.rpc.username}")
    private val username: String? = null
    // The username for logging into the RPC client.
    @Value("\${config.rpc.password}")
    private val password: String? = null
    // The password for logging into the RPC client.
    @Value("\${config.rpc.port}")
    private val rpcPort: Int = 0

    lateinit var rpcConnection: CordaRPCConnection
        private set
    lateinit var proxy: CordaRPCOps
        private set

    @PostConstruct
    fun initialiseNodeRPCConnection() {

        val rpcAddress = NetworkHostAndPort(host.toString(), rpcPort)
        val rpcClient = CordaRPCClient(rpcAddress)
        val rpcConnection = rpcClient.start(username.toString(), password.toString())
        proxy = rpcConnection.proxy
    }

    @PreDestroy
    override fun close() {
        rpcConnection.notifyServerAndClose()
    }
}