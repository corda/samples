package net.corda.examples.oracle.service.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.oracle.base.flow.QueryPrime;
import net.corda.examples.oracle.service.service.Oracle;
import org.jetbrains.annotations.Nullable;

@InitiatedBy(QueryPrime.class)
public class QueryHandler extends FlowLogic<Void> {
    private static ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving query request.");
    private static ProgressTracker.Step CALCULATING = new ProgressTracker.Step("Calculating Nth prime.");
    private static ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending query response.");

    private final ProgressTracker progressTracker = new ProgressTracker(RECEIVING, CALCULATING, SENDING);

    private final FlowSession session;

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public QueryHandler(FlowSession session) {
        this.session = session;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        progressTracker.setCurrentStep(RECEIVING);
        Integer request = session.receive(Integer.class).unwrap(it -> it);

        progressTracker.setCurrentStep(CALCULATING);
        Integer response;
        try {
            // Get the nth prime from the oracle.
            response = getServiceHub().cordaService(Oracle.class).query(request);
        } catch (Exception e) {
            // Re-throw the exception as a FlowException so its propagated to the querying node.
            throw new FlowException(e);
        }

        progressTracker.setCurrentStep(SENDING);
        session.send(response);
        return null;
    }
}
