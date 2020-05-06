package com.example.flow;


import co.paralleluniverse.fibers.Suspendable;
import com.example.service.SalaryRateOracle;
import kotlin.Unit;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.utilities.ProgressTracker;

@InitiatedBy(SignRate.class)
class SignRateHandler extends FlowLogic<Unit> {
    private ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving sign request");
    private ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing filtered transaction.");
    private ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending sign response.");
    private FlowSession session;

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECEIVING,
            SIGNING,
            SENDING
    );

    public SignRateHandler(FlowSession session) {
        this.session = session;
    }

    @Suspendable
    @Override
    public Unit call() throws FlowException {
        progressTracker.setCurrentStep(RECEIVING);
        FilteredTransaction request = session.receive(FilteredTransaction.class).unwrap(it -> it);

        progressTracker.setCurrentStep(SIGNING);

        try{
            TransactionSignature response = getServiceHub().cordaService(SalaryRateOracle.class).sign(request);
            progressTracker.setCurrentStep(SENDING);
            session.send(response);
        } catch(Exception e ){
            throw new FlowException(e);
        };
        return null;
    }
}