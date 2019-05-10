package net.obligation.rpcClient

import net.corda.client.rpc.internal.ReconnectingCordaRPCOps
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.contextLogger

class RpcClient(config: RpcClientConfig) {

    companion object {
        private val log = contextLogger()
    }

    private val partyToConnection = config.partyToRpcPort.map { (party, networkHostAndPort) ->
        val nodeAddress = NetworkHostAndPort.parse(networkHostAndPort)
        val connection = ReconnectingCordaRPCOps(nodeAddress, "user1", "test")
        party to connection
    }.toMap().toMutableMap()

    private val connections = partyToConnection.values

    fun getRPCProxy(party: String): CordaRPCOps {
        return partyToConnection.getValue(party)
    }

    fun uploadAttachmentsToAllNodes() {
        // The attachments cannot simply be opened here, as the first node the attachment is uploaded to will close it.
        // Instead, create a mapping of attachment ids to a connection that can be used to open it.
        val attachmentLocations = connections.flatMap { connection ->
            val ids = connection.queryAttachments(AttachmentQueryCriteria.AttachmentsQueryCriteria(), null)
            ids.map { Pair(it, connection) }
        }.toMap()

        connections.forEach { connection ->
            attachmentLocations.forEach { (id, conn) ->
                if (!connection.attachmentExists(id)) {
                    val attachment = conn.openAttachment(id)
                    connection.uploadAttachment(attachment)
                }
            }
        }
    }

    fun closeAllConnections() {
        connections.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                log.warn("An error occurred while closing the connection: ${e.message}", e)
            }
        }
    }

    fun stopNodes() {
        connections.forEach {
            try {
                it.shutdown()
                it.close()
            } catch (e: Exception) {
                log.warn("An error occurred while shutting down a node: ${e.message}", e)
            }
        }
        partyToConnection.clear()
    }
}

data class RpcClientConfig(val partyToRpcPort: Map<String, String>)