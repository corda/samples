package net.corda.demos.crowdFunding.structures

import net.corda.core.contracts.StateAndRef
import net.corda.core.serialization.CordaSerializable
import net.corda.finance.contracts.asset.Cash
import java.security.PublicKey

/**
 * A payload to transport the cash states from the pledger to the manager. We have to do this due to the way the [Cash]
 * contract is currently implemented. It can only have one move command and it is impossible to remove commands from
 */

@CordaSerializable
class CashStatesPayload(
        val inputs: List<StateAndRef<Cash.State>>,
        val outputs: List<Cash.State>,
        val signingKeys: List<PublicKey>
)