package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(CreateAndPublishTenderFlow.class)
public class CreateAndPublishTenderResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession counterPartySession;

    public CreateAndPublishTenderResponderFlow(FlowSession counterPartySession) {
        this.counterPartySession = counterPartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(counterPartySession, null, StatesToRecord.ALL_VISIBLE));
    }
}
