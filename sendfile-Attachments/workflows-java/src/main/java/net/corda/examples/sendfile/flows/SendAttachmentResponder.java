package net.corda.examples.sendfile.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

// *********
// * Flows *
// *********
@InitiatedBy(SendAttachment.class)
public class SendAttachmentResponder extends FlowLogic<Void> {

    private final FlowSession counterPartySession;

    public SendAttachmentResponder(FlowSession counterPartySession) {
        this.counterPartySession = counterPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Responder flow logic goes here.
        SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterPartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                if (stx.getTx().getAttachments().isEmpty())
                    throw new FlowException("No Jar was being sent");
            }
        });

        subFlow(new ReceiveFinalityFlow(counterPartySession, signedTransaction.getId()));
        return null;
    }
}
