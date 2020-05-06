package com.flowdb;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;

import java.sql.SQLException;

/**
 * Updates the value of a crypto token in the table of crypto values.
 */
@InitiatingFlow
@StartableByRPC
public class UpdateTokenValueFlow extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final String token;
    private final Integer value;

    public UpdateTokenValueFlow(String token, Integer value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        final CryptoValuesDatabaseService databaseService = getServiceHub().cordaService(CryptoValuesDatabaseService.class);
        try {
            databaseService.updateTokenValue(token, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}