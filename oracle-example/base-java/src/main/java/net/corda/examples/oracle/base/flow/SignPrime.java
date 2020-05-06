package net.corda.examples.oracle.base.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;

// Simple flow which takes a filtered transaction (exposing only a command containing the nth prime data) and returns
// a digital signature over the transaction Merkle root.
@InitiatingFlow
public class SignPrime extends FlowLogic<TransactionSignature> {
    private final Party oracle;
    private final FilteredTransaction ftx;

    public SignPrime(Party oracle, FilteredTransaction ftx) {
        this.oracle = oracle;
        this.ftx = ftx;
    }

    @Suspendable
    @Override
    public TransactionSignature call() throws FlowException {
        FlowSession session = initiateFlow(oracle);
        return session.sendAndReceive(TransactionSignature.class, ftx).unwrap(it -> it);
    }
}
