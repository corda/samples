package net.corda.examples.whistleblower.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

@InitiatedBy(BlowWhistleFlow.class)
public class BlowWhistleFlowResponder extends FlowLogic<Void> {
    private final FlowSession counterpartySession;

    public BlowWhistleFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        subFlow(new SwapIdentitiesFlow(counterpartySession));

        SignedTransaction stx = subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // No checking need to be done.
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession, stx.getId()));
        return null;
    }
}
