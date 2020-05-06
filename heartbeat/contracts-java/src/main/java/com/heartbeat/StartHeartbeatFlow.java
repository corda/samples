package com.heartbeat;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Collections;

/**
 * Creates a Heartbeat state on the ledger.
 *
 * Every Heartbeat state has a scheduled activity to start a flow to consume itself and produce a
 * new Heartbeat state on the ledger after five seconds.
 *
 * By consuming the existing Heartbeat state and creating a new one, a new scheduled activity is
 * created.
 */
@InitiatingFlow
@StartableByRPC
public class StartHeartbeatFlow extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = tracker();

    private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating a HeartState transaction");
    private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with out private key.");
    private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    private static ProgressTracker tracker() {
        return new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        HeartState output = new HeartState(getOurIdentity());
        CommandData cmd = new HeartContract.Commands.Beat();
        TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                .addOutputState(output, HeartContract.contractID)
                .addCommand(cmd, getOurIdentity().getOwningKey());

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        subFlow(new FinalityFlow(signedTx, Collections.emptyList(), FINALISING_TRANSACTION.childProgressTracker()));
        return null;
    }
}
