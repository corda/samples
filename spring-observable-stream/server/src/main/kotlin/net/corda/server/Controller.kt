package net.corda.server

import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.yo.YoFlow
import net.corda.yo.YoState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


private const val CONTROLLER_NAME = "config.controller.name"

/**
 *  A controller for interacting with the node via RPC.
 */
@RestController
@RequestMapping("/yo") // The paths for GET and POST requests are relative to this base path.
private class RestController(
        private val rpc: NodeRPCConnection,
        private val template: SimpMessagingTemplate,
        @Value("\${$CONTROLLER_NAME}") private val controllerName: String) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myName = rpc.proxy.nodeInfo().legalIdentities.first().name

    // Upon creation, the controller starts streaming information on new Yo states to a websocket.
    // The front-end can subscribe to this websocket to be notified of updates.
    init {
        val yoStateVaultObservable = rpc.proxy.vaultTrack(YoState::class.java).updates
        yoStateVaultObservable.subscribe { update ->
            update.produced.forEach { (state) ->
                val yoStateJson = state.data.toJson()
                template.convertAndSend("/stompresponse", yoStateJson)
            }
        }
    }

    /** Maps a YoState to a JSON object. */
    private fun YoState.toJson(): Map<String, String> {
        return mapOf("origin" to origin.name.organisation, "target" to target.name.toString(), "yo" to yo)
    }

    /** Returns the node's name. */
    @GetMapping(value = "/myname", produces = arrayOf("text/plain"))
    private fun myName() = myName.toString()

    /** Returns a list of the node's network peers. */
    @GetMapping(value = "/peersnames", produces = arrayOf("application/json"))
    private fun peersNames(): Map<String, List<String>> {
        val nodes = rpc.proxy.networkMapSnapshot()
        val nodeNames = nodes.map { it.legalIdentities.first().name }
        val filteredNodeNames = nodeNames.filter { it.organisation !in listOf(controllerName, myName) }
        val filteredNodeNamesToStr = filteredNodeNames.map { it.toString() }
        return mapOf("peers" to filteredNodeNamesToStr)
    }

    /** Returns a list of existing Yo's. */
    @GetMapping(value = "/getyos", produces = arrayOf("application/json"))
    private fun getYos(): List<Map<String, String>> {
        val yoStateAndRefs = rpc.proxy.vaultQueryBy<YoState>().states
        val yoStates = yoStateAndRefs.map { it.state.data }
        return yoStates.map { it.toJson() }
    }

    /** Sends a Yo to a counterparty. */
    @PostMapping(value = "/sendyo", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=application/x-www-form-urlencoded"))
    private fun sendYo(request: HttpServletRequest): ResponseEntity<String> {
        val targetName = request.getParameter("target")
        val targetX500Name = CordaX500Name.parse(targetName)
        val target = rpc.proxy.wellKnownPartyFromX500Name(targetX500Name) ?: throw IllegalArgumentException("Unrecognised peer.")
        val flow = rpc.proxy.startFlowDynamic(YoFlow::class.java, target)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("You just sent a Yo! to ${target.name}")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Yo! was invalid - ${e.cause?.message}")
        }
    }
}