package net.corda.demos.crowdFunding.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.node.StatesToRecord

/**
 * Other side of the [BroadcastTransaction] flow. It uses the observable states feature. When [ReceiveTransactionFlow]
 * is called, the [StatesToRecord.ALL_VISIBLE] parameter is used so that all the states are recorded despite the
 * receiving node not being a participant in these states.
 *
 * WARNING: This feature still needs work. Storing fungible states, like cash when you are not the owner will cause
 * problems when using [generateSpend] as the vault currently assumes that all states in the vault are spendable. States
 * you are only an observer of are NOT spendable!
 */
@InitiatedBy(BroadcastTransaction::class)
class RecordTransactionAsObserver(val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        // Receive and record the new campaign state in our vault EVEN THOUGH we are not a participant as we are
        // using 'ALL_VISIBLE'.
        val flow = ReceiveTransactionFlow(
                otherSideSession = otherSession,
                checkSufficientSignatures = true,
                statesToRecord = StatesToRecord.ALL_VISIBLE
        )

        subFlow(flow)
    }

}