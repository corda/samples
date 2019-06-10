package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.gitcoins.states.GitToken
import com.gitcoins.utilities.QueryGitUserDatabase
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import net.corda.core.crypto.Crypto
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.SignedTransaction

/**
 * Flow that delegates the issuing of a [GitToken] to the [IssueToken] subflow. This flow is triggered by a GitHub pull
 * request review event that is configured on a GitHub webhook.
 */
@StartableByRPC
class PullRequestReviewEventFlow(private val gitUserName: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {

        val result = QueryGitUserDatabase().listEntriesForGitUserName(gitUserName, serviceHub)

        if (result.isEmpty()) {
            throw FlowException("No public key for git username '$gitUserName'. \n " +
                    "Please comment 'createKey' on a PR to generate a public key for '$gitUserName'.")
        }

        val token = GitToken()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val key = Crypto.decodePublicKey(result.first().userKey)

        //TODO Implement evaluation logic based on the commit

        val party = serviceHub.identityService.wellKnownPartyFromAnonymous(AnonymousParty(key))
        if (party != null) {
            return subFlow(IssueToken.Initiator(token, party, notary, 1 of token))
        } else throw FlowException("A well known party was not found using public key: $key")
    }
}