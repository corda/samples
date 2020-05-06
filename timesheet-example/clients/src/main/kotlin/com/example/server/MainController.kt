package com.example.server

import com.example.flow.IssueInvoiceFlow
import com.example.flow.PayInvoiceFlow
import com.example.state.InvoiceState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.servlet.http.HttpServletRequest

val SERVICE_NAMES = listOf("Notary", "Network Map Service", "Oracle")

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/api/example/") // The paths for GET and POST requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GetMapping(value = [ "invoices" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getInvoices() : ResponseEntity<List<StateAndRef<InvoiceState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<InvoiceState>().states)
    }

    /**
     * Initiates a flow to issue an Invoice between two parties.
     *
     * Once the flow finishes it will have written the Invoice to ledger. Both the contractor and the company will be able to
     * see it when calling /spring/api/invoices on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */

    @PostMapping(value = [ "create-invoice" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createInvoice(request: HttpServletRequest): ResponseEntity<String> {
        val hoursWorked = request.getParameter("hoursWorked").toInt()
        val date = LocalDate.parse(request.getParameter("date").substringBefore("00").trim(), DateTimeFormatter.ofPattern("E MMMM d yyyy"))
        val partyName = request.getParameter("megacorp") ?: return ResponseEntity.badRequest().body("Query parameter 'MegaCorp' must not be null.\n")
        if (hoursWorked <= 0 ) {
            return ResponseEntity.badRequest().body("Query parameter 'hoursWorked' must be non-negative.\n")
        }
        val partyX500Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(IssueInvoiceFlow::Initiator, hoursWorked, date, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @PostMapping(value = [ "pay-invoice" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun payInvoice(request: HttpServletRequest): ResponseEntity<String> {
        val invoiceId = UUID.fromString(request.getParameter("invoiceId"))

        return try {
            val signedTx = proxy.startTrackedFlow(PayInvoiceFlow::Initiator, invoiceId).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * Displays all Invoice states that only this node has been involved in.
     */
    @GetMapping(value = [ "my-invoices" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getMyInvoices(): ResponseEntity<List<StateAndRef<InvoiceState>>>  {
        val myInvoices = proxy.vaultQueryBy<InvoiceState>().states.filter { it.state.data.participants.contains(proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myInvoices)
    }

}
