package net.corda.server

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"
private const val CORDA_NODE_HOST = "config.rpc.host"
private const val CORDA_RPC_PORT = "config.rpc.port"

/**
 * Wraps a node RPC proxy.
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
open class NodeRPCConnection(
        @Value("\${$CORDA_NODE_HOST}") host: String,
        @Value("\${$CORDA_USER_NAME}") username: String,
        @Value("\${$CORDA_USER_PASSWORD}") password: String,
        @Value("\${$CORDA_RPC_PORT}") rpcPort: Int) {

    val proxy: CordaRPCOps

    init {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        val rpcClient = CordaRPCClient(rpcAddress)
        val rpcConnection = rpcClient.start(username, password)
        proxy = rpcConnection.proxy
    }
}