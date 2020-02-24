package com.observable.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

@InitiatingFlow
public class ReportManually extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final SignedTransaction signedTransaction;
    private final Party regulator;

    public ReportManually(SignedTransaction signedTransaction, Party regulator) {
        this.signedTransaction = signedTransaction;
        this.regulator = regulator;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        FlowSession session = initiateFlow(regulator);
        session.send(signedTransaction);
        return null;
    }
}
