package net.corda.examples.oracle.service.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.oracle.base.flow.SignPrime;
import net.corda.examples.oracle.service.service.Oracle;
import org.jetbrains.annotations.Nullable;

@InitiatedBy(SignPrime.class)
public class SignHandler extends FlowLogic<Void> {
    private static final ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving sign request.");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing filtered transaction.");
    private static final ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending sign response.");

    private final ProgressTracker progressTracker = new ProgressTracker(RECEIVING, SIGNING, SENDING);

    private final FlowSession session;

    public SignHandler(FlowSession session) {
        this.session = session;
    }

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        progressTracker.setCurrentStep(RECEIVING);
        FilteredTransaction request = session.receive(FilteredTransaction.class).unwrap(it -> it);

        progressTracker.setCurrentStep(SIGNING);
        TransactionSignature response;
        try {
            response = getServiceHub().cordaService(Oracle.class).sign(request);
        } catch (FilteredTransactionVerificationException e) {
            throw new FlowException(e);
        }

        progressTracker.setCurrentStep(SENDING);
        session.send(response);
        return null;
    }
}
