package net.obligation.rpcClient

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
    val nodes = Pairs(listOf("PartyA", "PartyB", "PartyC"))
    for ((node, lender) in nodes) {
        log.info("Creating obligation. Lender: $lender, borrower: $node")
        val nodeConn = client.getConnection(node)
        // TODO: ensure there is only one party here.
        val lenderParty = nodeConn.proxy.partiesFromName(lender, true).first()
        nodeConn.proxy.startFlowDynamic(IssueObligation.Initiator::class.java, 100.DOLLARS, lenderParty, true).returnValue.getOrThrow()
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

class Pairs<T>(private val list: List<T>): Iterator<Pair<T, T>> {

    private var firstIdx = 0
    private var secondIdx = 1
    override fun hasNext(): Boolean {
        return firstIdx < list.size
    }

    override fun next(): Pair<T, T> {
        val nextItem = Pair(list[firstIdx], list[secondIdx])
        secondIdx++
        if (secondIdx == firstIdx) {
            secondIdx++
        }
        if (secondIdx >= list.size) {
            secondIdx = 0
            firstIdx++
        }
        return nextItem
    }
}