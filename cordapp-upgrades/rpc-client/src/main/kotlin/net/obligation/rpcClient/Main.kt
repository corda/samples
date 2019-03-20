package net.obligation.rpcClient

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCClientConfiguration
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.utilities.NetworkHostAndPort

fun main(args: Array<String>) {
    val parties = hashMapOf("PartyA" to "localhost:10006", "PartyB" to "localhost:10009", "PartyC" to "localhost:10012")
    val connections = parties.map { (party, networkHostAndPort) ->
        val nodeAddress = NetworkHostAndPort.parse(networkHostAndPort)
        val connection = CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT).start("user1", "test")
        Pair(party, connection)
    }.toMap()

    val attachments = connections.flatMap { (_, connection) ->
        connection.proxy.queryAttachments(AttachmentQueryCriteria.AttachmentsQueryCriteria(), null)
    }.toSet()

    connections.forEach { (_, connection) ->
        attachments.forEach { attachmentId ->
            if (!connection.proxy.attachmentExists(attachmentId)) {
                importAttachment(connection, attachmentId)
            }
        }
    }
}

fun importAttachment(rpcConnection: CordaRPCConnection, attachmentId: SecureHash) {
    val attachmentStream = rpcConnection.proxy.openAttachment(attachmentId)
    rpcConnection.proxy.uploadAttachment(attachmentStream)
}