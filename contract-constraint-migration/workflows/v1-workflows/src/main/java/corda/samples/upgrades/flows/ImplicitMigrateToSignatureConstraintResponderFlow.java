package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

@InitiatedBy(ImplicitMigrateToSignatureConstraintFlow.class)
public class ImplicitMigrateToSignatureConstraintResponderFlow extends FlowLogic<SignedTransaction> {

    private FlowSession counterPartySession;

    public ImplicitMigrateToSignatureConstraintResponderFlow(FlowSession counterPartySession) {
        this.counterPartySession = counterPartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                super(otherPartyFlow, progressTracker);
            }

            @Override
            protected void checkTransaction(SignedTransaction stx) {

            }
        }

        final SignTxFlow signTxFlow = new SignTxFlow(counterPartySession, SignTransactionFlow.Companion.tracker());
        final SecureHash txId = subFlow(signTxFlow).getId();

        return subFlow(new ReceiveFinalityFlow(counterPartySession, txId));
    }
}
