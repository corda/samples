package net.corda.demos.crowdFunding.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.transactions.SignedTransaction

/**
 * Filters out any notary identities and removes our identity, then broadcasts the [SignedTransaction] to all the
 * remaining identities.
 */
@InitiatingFlow
class BroadcastTransaction(val stx: SignedTransaction) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        // Get a list of all identities from the network map cache.
        val everyone = serviceHub.networkMapCache.allNodes.flatMap { it.legalIdentities }

        // Filter out the notary identities and remove our identity.
        val everyoneButMeAndNotary = everyone.filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity

        // Create a session for each remaining party.
        val sessions = everyoneButMeAndNotary.map { initiateFlow(it) }

        // Send the transaction to all the remaining parties.
        sessions.forEach { subFlow(SendTransactionFlow(it, stx)) }
    }

}