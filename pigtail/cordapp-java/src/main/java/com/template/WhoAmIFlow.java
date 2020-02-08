package com.template;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByService;

/**
 * A regular flow that returns our node's name.
 */
@InitiatingFlow
@StartableByService
public class WhoAmIFlow extends FlowLogic<String> {
    @Suspendable
    @Override
    public String call() throws FlowException {
        return getOurIdentity().getName().getOrganisation();
    }
}
