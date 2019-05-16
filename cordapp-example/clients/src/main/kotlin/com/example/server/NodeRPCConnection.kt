package com.example.server

import net.corda.client.rpc.internal.ReconnectingCordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"
private const val CORDA_NODE_HOST = "config.rpc.host"
private const val CORDA_RPC_PORT = "config.rpc.port"

/**
 * Wraps a node RPC proxy.
 *
 * The RPC proxy is configured based on the properties in `application.properties`.
 *
 * @property host The host of the node we are connecting to.
 * @property rpcPort The RPC port of the node we are connecting to.
 * @property username The username for logging into the RPC client.
 * @property password The password for logging into the RPC client.
 * @property proxy The RPC proxy.
 */
@Component
open class NodeRPCConnection(
        @Value("\${$CORDA_NODE_HOST}") private val host: String,
        @Value("\${$CORDA_USER_NAME}") private val username: String,
        @Value("\${$CORDA_USER_PASSWORD}") private val password: String,
        @Value("\${$CORDA_RPC_PORT}") private val rpcPort: Int) {

    lateinit var proxy: ReconnectingCordaRPCOps
        private set

    @PostConstruct
    fun initialiseNodeRPCConnection() {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        proxy = ReconnectingCordaRPCOps(rpcAddress, username, password)
    }

    @PreDestroy
    fun close() {
        proxy.close()
    }
}