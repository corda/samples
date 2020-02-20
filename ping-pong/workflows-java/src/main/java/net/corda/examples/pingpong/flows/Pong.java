package net.corda.examples.pingpong.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.utilities.UntrustworthyData;

@InitiatedBy(Ping.class)
public class Pong extends FlowLogic<Void> {

    private final FlowSession counterpartySession;

    public Pong(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;

    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        UntrustworthyData<String> counterpartyData = counterpartySession.receive(String.class);
        counterpartyData.unwrap(msg -> {
            assert (msg.equals("ping"));
            return true;
        });
        counterpartySession.send("pong");
        return null;
    }
}
