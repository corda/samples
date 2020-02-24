package net.corda.examples.attachments;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.attachments.states.AgreementState;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;

@InitiatedBy(ProposeFlow.class)
public class AgreeFlow extends FlowLogic<SignedTransaction> {
    FlowSession counterPartySession = null;

    public AgreeFlow(FlowSession counterPartySession) {
        this.counterPartySession = counterPartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        FlowLogic<SignedTransaction> signTransactionFlow = new SignTransactionFlow(counterPartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // We ensure that the transaction contains an AgreementContract
                try {
                    if (stx.toLedgerTransaction(getServiceHub(), false).outputsOfType(AgreementState.class).isEmpty()) {
                        throw new FlowException("Agreement transaction did not contain an output AgreementState.");
                    }
                } catch (SignatureException e) {
                    e.printStackTrace();
                }
            }
        };

        final SecureHash txId = subFlow(signTransactionFlow).getId();
        return subFlow(new ReceiveFinalityFlow(counterPartySession, txId));
    }
}
