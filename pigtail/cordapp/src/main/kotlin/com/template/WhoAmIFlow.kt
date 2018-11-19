package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByService

/**
 * A regular flow that returns our node's name.
 */
@InitiatingFlow
@StartableByService
class WhoAmIFlow : FlowLogic<String>() {
    @Suspendable
    override fun call() : String {
        return ourIdentity.name.organisation
    }
}