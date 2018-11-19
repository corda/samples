package net.corda.option.base.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.unwrap

/** Called by the client to request the oracle's signature over a filtered transaction. */
@InitiatingFlow
class RequestOracleSig(private val oracle: Party, private val ftx: FilteredTransaction) : FlowLogic<TransactionSignature>() {
    @Suspendable override fun call(): TransactionSignature {
        val oracleSession = initiateFlow(oracle)
        return oracleSession.sendAndReceive<TransactionSignature>(ftx).unwrap { it }
    }
}