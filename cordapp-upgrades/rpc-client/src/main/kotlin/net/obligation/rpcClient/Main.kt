package net.obligation.rpcClient

import com.google.common.collect.Sets
import joptsimple.OptionParser
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.examples.obligation.contract.Obligation
import net.corda.examples.obligation.workflow.flows.IssueObligation
import net.corda.examples.obligation.workflow.flows.SettleObligation
import net.corda.finance.DOLLARS
import net.corda.finance.flows.CashIssueFlow
import java.util.logging.Logger
import kotlin.system.exitProcess

val log: Logger = Logger.getLogger("root")

fun main(args: Array<String>) {
    val parser = OptionParser()
    val modeOption = parser.accepts("mode").withRequiredArg().ofType(Modes::class.java).describedAs(Modes.description()).required()
    val options = try {
        parser.parse(*args)
    } catch (e: Exception) {
        println(e.message)
        exitProcess(1)
    }
    val mode = options.valueOf(modeOption)
    val config = RpcClientConfig(
            hashMapOf("PartyA" to "localhost:10006", "PartyB" to "localhost:10009", "PartyC" to "localhost:10012", "Notary" to "localhost:10003")
    )
    val client = RpcClient(config)

    when (mode) {
        Modes.UPLOAD_ATTACHMENTS -> client.uploadAttachmentsToAllNodes()
        Modes.ISSUE_BETWEEN_NODES -> {
            client.uploadAttachmentsToAllNodes()
            issueBetweenAllNodes(client)
        }
        Modes.SETTLE_ALL_OBLIGATIONS -> {
            client.uploadAttachmentsToAllNodes()
            settleAllObligations(client)
        }
        null -> throw Exception("Mode is missing from input arguments")
    }
    client.closeAllConnections()
}

/**
 * Issue an obligation between all pairs of nodes.
 */
fun issueBetweenAllNodes(client: RpcClient) {
    fun createObligation(lender: String, borrower: String) {
        log.info("Creating obligation. Lender: $lender, borrower: $borrower")
        val nodeConn = client.getConnection(borrower)
        // TODO: ensure there is only one party here.
        val lenderParty = nodeConn.proxy.partiesFromName(lender, true).first()
        nodeConn.proxy.startFlowDynamic(IssueObligation.Initiator::class.java, 100.DOLLARS, lenderParty, true).returnValue.getOrThrow()
    }
    val nodePairs = Sets.combinations(mutableSetOf("PartyA", "PartyB", "PartyC"), 2)
    for (nodes in nodePairs) {
        val nodeList = nodes.toList()
        val (firstNode, secondNode) = Pair(nodeList.first(), nodeList.last())
        createObligation(firstNode, secondNode)
        createObligation(secondNode, firstNode)
    }
}

fun settleAllObligations(client: RpcClient) {
    val nodes = listOf("PartyA", "PartyB", "PartyC")
    for (node in nodes) {
        val nodeConn = client.getConnection(node)
        val nodeParties = nodeConn.proxy.nodeInfo().legalIdentities // Node only has one party.
        val obligations = nodeConn.proxy.vaultQuery(Obligation::class.java).states
                .map { it.state.data }
                .filter {
                    val borrowerParty = nodeConn.proxy.wellKnownPartyFromAnonymous(it.borrower)
                    nodeParties.any { party -> party == borrowerParty }
                }
        val notary = nodeConn.proxy.notaryIdentities().first()
        obligations.forEach {
            nodeConn.proxy.startFlowDynamic(CashIssueFlow::class.java, it.amount, OpaqueBytes.of(123), notary).returnValue.getOrThrow()
            nodeConn.proxy.startFlowDynamic(SettleObligation.Initiator::class.java, it.linearId, it.amount, true).returnValue.getOrThrow()
        }
    }
}

enum class Modes {
    UPLOAD_ATTACHMENTS,
    ISSUE_BETWEEN_NODES,
    SETTLE_ALL_OBLIGATIONS;

    companion object {
        fun description(): String {
            return values().joinToString(prefix = "[", postfix = "]") { it.name }
        }
    }
}