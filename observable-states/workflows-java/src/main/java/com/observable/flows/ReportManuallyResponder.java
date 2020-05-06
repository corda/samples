package com.observable.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(ReportManually.class)
public class ReportManuallyResponder extends FlowLogic<Void> {
    private final FlowSession counterpartySession;

    public ReportManuallyResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        SignedTransaction signedTransaction = counterpartySession.receive(SignedTransaction.class).unwrap(it -> it);
        // The national regulator records all of the transaction's states using
        // `recordTransactions` with the `ALL_VISIBLE` flag.
        getServiceHub().recordTransactions(StatesToRecord.ALL_VISIBLE, ImmutableList.of(signedTransaction));
        return null;
    }
}
