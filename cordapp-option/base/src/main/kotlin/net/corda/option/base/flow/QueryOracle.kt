package net.corda.option.base.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap
import net.corda.option.base.SpotPrice
import net.corda.option.base.Volatility
import java.time.Instant

/** Called by the client to request a stock's spot price and volatility at a point in time from an oracle. */
@InitiatingFlow
class QueryOracle(private val oracle: Party, private val stock: String, private val atTime: Instant) : FlowLogic<Pair<SpotPrice, Volatility>>() {
    @Suspendable override fun call(): Pair<SpotPrice, Volatility> {
        val oracleSession = initiateFlow(oracle)
        return oracleSession.sendAndReceive<Pair<SpotPrice, Volatility>>(Pair(stock, atTime)).unwrap { it }
    }
}