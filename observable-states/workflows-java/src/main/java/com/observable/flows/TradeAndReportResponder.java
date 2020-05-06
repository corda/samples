package com.observable.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;

@InitiatedBy(TradeAndReport.class)
public class TradeAndReportResponder extends FlowLogic<Void> {
    private final FlowSession counterpartySession;

    public TradeAndReportResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        subFlow(new ReceiveFinalityFlow(counterpartySession, null, StatesToRecord.ALL_VISIBLE));
        return null;
    }
}
