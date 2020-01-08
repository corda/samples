package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.service.Rate;
import com.example.service.RateOf;
import com.example.service.SalaryRateOracle;
import kotlin.Unit;
import net.corda.core.flows.*;
import net.corda.core.utilities.ProgressTracker;


@InitiatedBy(QueryRate.class)
public class QueryRateHandler extends FlowLogic<Unit>{
    private ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving query request");
    private ProgressTracker.Step CALCULATING = new ProgressTracker.Step("Checking salary table.");
    private ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending query response.");
    private FlowSession session;

    public QueryRateHandler(FlowSession session) {
        this.session = session;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECEIVING,
            CALCULATING,
            SENDING
    );



    @Suspendable
    @Override
    //TODO THS IS THE WRONG DAMN CLASS
    public Unit call() throws FlowException {
        progressTracker.setCurrentStep(RECEIVING);
        RateOf request = session.receive(RateOf.class).unwrap(it -> it);
        progressTracker.setCurrentStep(CALCULATING);
       try{
           Rate response = getServiceHub().cordaService(SalaryRateOracle.class).query(request);
           progressTracker.setCurrentStep(SENDING);
           session.send(response);
       } catch(Exception e ){
           throw new FlowException(e);
       };
       return null;

    }
}
