package net.obligation.rpcClient

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCClientConfiguration
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.utilities.NetworkHostAndPort

fun main(args: Array<String>) {
    val parties = hashMapOf("PartyA" to "localhost:10006", "PartyB" to "localhost:10009", "PartyC" to "localhost:10012")
    val connections = parties.map { (party, networkHostAndPort) ->
        val nodeAddress = NetworkHostAndPort.parse(networkHostAndPort)
        val connection = CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT).start("user1", "test")
        Pair(party, connection)
    }.toMap()

    // The attachments cannot simply be opened here, as the first node the attachment is uploaded to will close it.
    // Instead, create a mapping of attachment ids to a connection that can be used to open it.
    val attachmentLocations = connections.flatMap { (_, connection) ->
        val ids = connection.proxy.queryAttachments(AttachmentQueryCriteria.AttachmentsQueryCriteria(), null)
        ids.map { Pair(it, connection) }
    }.toMap()

    connections.forEach { (_, connection) ->
        attachmentLocations.forEach { (id, conn) ->
            if (!connection.proxy.attachmentExists(id)) {
                val attachment = conn.proxy.openAttachment(id)
                connection.proxy.uploadAttachment(attachment)
            }
        }
    }

    connections.forEach { (_, connection) -> connection.close() }
}