package net.corda.demos.crowdFunding

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import java.security.PublicKey

/** Pick out all the pledges for the specified campaign. */
fun pledgersForCampaign(services: ServiceHub, campaign: Campaign): List<StateAndRef<Pledge>> {
    val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
    return builder {
        val campaignReference = Pledge.PledgeSchemaV1.PledgeEntity::campaign_reference.equal(campaign.linearId.id.toString())
        val customCriteria = QueryCriteria.VaultCustomQueryCriteria(campaignReference)
        val criteria = generalCriteria `and` customCriteria
        services.vaultService.queryBy<Pledge>(criteria)
    }.states
}

/** Return a set of PublicKeys from the list of participants of a state. */
fun keysFromParticipants(obligation: ContractState): Set<PublicKey> {
    return obligation.participants.map {
        it.owningKey
    }.toSet()
}