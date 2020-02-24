package net.corda.examples.autopayroll.flows

import net.corda.examples.autopayroll.states.PaymentRequestState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.trackBy
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@CordaService
class AutoPaymentService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        val log = loggerFor<AutoPaymentService>()
        val executor: Executor = Executors.newFixedThreadPool(8)!!
    }

    init {
        directPayment()
        log.info("Tracking new Payment Request")
    }

    private fun directPayment() {
        val ourIdentity = ourIdentity()
        serviceHub.vaultService.trackBy<PaymentRequestState>().updates.subscribe {
            update: Vault.Update<PaymentRequestState> -> update.produced.forEach{
            message: StateAndRef<PaymentRequestState> ->
                val state = message.state
                if (ourIdentity == serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("BankOperator", "Toronto", "CA"))!!) {
                    executor.execute {
                        log.info("Directing to message $state")
                        serviceHub.startFlow(PaymentFlowInitiator())
                    }
                }
            }
        }
    }
    private fun ourIdentity(): Party = serviceHub.myInfo.legalIdentities.first()

}