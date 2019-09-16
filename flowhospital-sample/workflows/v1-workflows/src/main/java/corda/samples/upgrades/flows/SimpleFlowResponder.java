package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(SimpleFlowInitiator.class)
public class SimpleFlowResponder extends FlowLogic<SignedTransaction> {

    private FlowSession counterPartySession;

    public SimpleFlowResponder(FlowSession counterPartySession) {
        this.counterPartySession = counterPartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        Integer it = counterPartySession.receive(Integer.class).unwrap(flag -> flag);
        if(it == 1)
                throw new IllegalArgumentException("This is thrown as UnexpectedFlowEndException and this actual message is not thrown to the party waiting on receive on counterPartySession");
        if(it == 2)
                throw new FlowException("This exception message is propogated to the party waiting on receive on counterPartySessione ");
        return null;
    }
}
