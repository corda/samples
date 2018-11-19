package net.corda.option.client.flow

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

fun ServiceHub.firstNotary() = networkMapCache.notaryIdentities.first()

fun ServiceHub.firstIdentityByName(name: CordaX500Name) = networkMapCache.getNodeByLegalName(name)?.legalIdentities?.first()
        ?: throw IllegalArgumentException("Requested oracle $name not found on network.")

inline fun <reified T : ContractState> ServiceHub.getStateAndRefByLinearId(linearId: UniqueIdentifier): StateAndRef<T> {
    val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
    return vaultService.queryBy<T>(queryCriteria).states.single()
}