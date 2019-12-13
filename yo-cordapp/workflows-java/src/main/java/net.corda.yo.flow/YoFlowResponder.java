package net.corda.yo.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(YoFlow.class)
public class YoFlowResponder extends FlowLogic<SignedTransaction> {
    private FlowSession counterpartySession;

    YoFlowResponder(FlowSession counterpartySession){
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(this.counterpartySession));
    }
}



