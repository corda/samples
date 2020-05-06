package com.heartbeat;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;

import java.util.Collections;

/**
 * This is the flow that a Heartbeat state runs when it consumes itself to create a new Heartbeat
 * state on the ledger.
 */
@InitiatingFlow
@SchedulableFlow
public class HeartbeatFlow extends FlowLogic<String> {
    private final StateRef stateRef;
    private final ProgressTracker progressTracker = tracker();

    private static final Step GENERATING_TRANSACTION = new Step("Generating a HeartState transaction");
    private static final Step SIGNING_TRANSACTION = new Step("Signing transaction with out private key.");
    private static final Step FINALISING_TRANSACTION = new Step("Recording transaction") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    private ProgressTracker tracker() {
        return new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );
    }

    public HeartbeatFlow(StateRef stateRef) {
        this.stateRef = stateRef;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        StateAndRef<HeartState> input = getServiceHub().toStateAndRef(stateRef);
        HeartState output = new HeartState(getOurIdentity());
        CommandData beatCmd = new HeartContract.Commands.Beat();

        TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
            .addInputState(input)
            .addOutputState(output, HeartContract.contractID)
            .addCommand(beatCmd, getOurIdentity().getOwningKey());

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        return "Lub-dub";
    }
}
