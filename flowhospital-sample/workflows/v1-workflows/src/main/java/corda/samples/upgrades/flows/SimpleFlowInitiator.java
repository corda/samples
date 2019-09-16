package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

@InitiatingFlow
@StartableByRPC
public class SimpleFlowInitiator extends FlowLogic<SignedTransaction> {

    private Party counterParty;
    private int number;

    public SimpleFlowInitiator(Party counterParty, int number) {
        this.counterParty = counterParty;
        this.number = number;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        FlowSession session = initiateFlow(counterParty);

        ////1
        session.send(number);

        session.receive(Integer.class);

        return null;
    }
}
