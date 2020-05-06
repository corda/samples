package net.corda.examples.pingpong.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.utilities.UntrustworthyData;

@InitiatingFlow
@StartableByRPC
public class Ping extends FlowLogic<Void> {

    private final Party counterparty;

    public Ping(Party counterparty) {
        this.counterparty = counterparty;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        final FlowSession counterpartySession = initiateFlow(counterparty);
        final UntrustworthyData<String> counterpartyData = counterpartySession.sendAndReceive(String.class, "ping");
        counterpartyData.unwrap( msg -> {
            assert(msg.equals("pong"));
            return true;
        });
        return null;
    }
}
