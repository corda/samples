package com.whistleblower

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
class BlowWhistleStateObserver(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        val log = loggerFor<BlowWhistleStateObserver>()
        val executor: Executor = Executors.newFixedThreadPool(8)!!
    }

    init {
        directNewBlowState()
        log.info("Tracking new Blow Whistle State")
    }

    private fun directNewBlowState() {
        val ourIdentity = ourIdentity()
        serviceHub.vaultService.trackBy<BlowWhistleState>().updates.subscribe { update: Vault.Update<BlowWhistleState> ->
            update.produced.forEach { message: StateAndRef<BlowWhistleState> ->
                val state = message.state.data
                if (ourIdentity == serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("SurveyMonkey", "SanFrancisco", "KE"))!!) {
                    executor.execute {
                        log.info("Directing to message ${message.state.data}")
                        serviceHub.startFlow(AutoDirectFlow())
                    }
                }
            }
        }
    }
    private fun ourIdentity(): Party = serviceHub.myInfo.legalIdentities.first()

}