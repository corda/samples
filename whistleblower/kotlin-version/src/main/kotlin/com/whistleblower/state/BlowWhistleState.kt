package com.whistleblower.state

import com.whistleblower.contract.BlowWhistleContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party

/**
 * A state representing a whistle-blowing case.
 *
 * The identity of both the whistle-blower and the investigator is kept confidential through the
 * use of [AnonymousParty].
 *
 * @property badCompany the company the whistle is being blown on.
 * @property whistleBlower the [AnonymousParty] blowing the whistle.
 * @property investigator the [AnonymousParty] handling the investigation.
 */
@BelongsToContract(BlowWhistleContract::class)
data class BlowWhistleState(
        val badCompany: Party,
        val whistleBlower: AnonymousParty,
        val investigator: AnonymousParty,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    // Both the whistle-blower and the current investigator are kept in the loop.
    override val participants = listOf(whistleBlower, investigator)
}