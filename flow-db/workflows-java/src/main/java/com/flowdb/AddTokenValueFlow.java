package com.flowdb;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;

import java.sql.SQLException;

/**
 * Adds a crypto token and associated value to the table of crypto values.
 */
@InitiatingFlow
@StartableByRPC
public class AddTokenValueFlow extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final String token;
    private final Integer value;

    public AddTokenValueFlow(String token, Integer value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        final CryptoValuesDatabaseService databaseService = getServiceHub().cordaService(CryptoValuesDatabaseService.class);
        // BE CAREFUL when accessing the node's database in flows:
        // 1. The operation must be executed in a BLOCKING way. Flows don't
        //    currently support suspending to await a database operation's
        //    response
        // 2. The operation must be idempotent. If the flow fails and has to
        //    restart from a checkpoint, the operation will also be replayed
        try {
            databaseService.addtokenValue(token, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
