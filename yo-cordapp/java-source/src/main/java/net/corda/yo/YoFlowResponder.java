package net.corda.yo;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.transactions.SignedTransaction;

public class YoFlowResponder extends FlowLogic<SignedTransaction> {
    private FlowSession counterpartySession;

    YoFlowResponder(FlowSession counterpartySession){
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}



