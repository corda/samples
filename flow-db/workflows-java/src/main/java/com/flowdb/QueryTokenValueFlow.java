package com.flowdb;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;

import java.sql.SQLException;

/**
 * Retrieves the value of a crypto token in the table of crypto values.
 */
@InitiatingFlow
@StartableByRPC
public class QueryTokenValueFlow extends FlowLogic<Integer> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final String token;

    public QueryTokenValueFlow(String token) {
        this.token = token;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Integer call() throws FlowException {
        final CryptoValuesDatabaseService databaseService = getServiceHub().cordaService(CryptoValuesDatabaseService.class);
        Integer val = null;
        try {
            val = databaseService.queryTokenValue(token);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return val;
    }
}
