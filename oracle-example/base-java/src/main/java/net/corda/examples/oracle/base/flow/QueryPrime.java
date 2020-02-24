package net.corda.examples.oracle.base.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;

// Simple flow that requests the Nth prime number from the specified oracle.
@InitiatingFlow
public class QueryPrime extends FlowLogic<Integer> {
    private final Party oracle;
    private final Integer n;

    public QueryPrime(Party oracle, Integer n) {
        this.oracle = oracle;
        this.n = n;
    }

    @Suspendable
    @Override
    public Integer call() throws FlowException {
        return initiateFlow(oracle).sendAndReceive(Integer.class, n).unwrap(it -> it);
    }
}
