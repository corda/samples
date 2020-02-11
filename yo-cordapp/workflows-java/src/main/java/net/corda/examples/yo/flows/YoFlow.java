package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.yo.contracts.YoContract;
import net.corda.examples.yo.states.YoState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class YoFlow extends FlowLogic<SignedTransaction> {
    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating a new Yo!");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the Yo!");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verfiying the Yo!");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending the Yo!") {
        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    ProgressTracker progressTracker = new ProgressTracker(
            CREATING,
            SIGNING,
            VERIFYING,
            FINALISING
    );

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private final Party target;

    public YoFlow(Party target) {
        this.target = target;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(CREATING);

        Party me = getOurIdentity();
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Command<YoContract.Commands.Send> command = new Command<YoContract.Commands.Send>(new YoContract.Commands.Send(), ImmutableList.of(me.getOwningKey()));
        YoState state = new YoState(me, target);
        StateAndContract stateAndContract = new StateAndContract(state, YoContract.ID);
        TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract, command);

        progressTracker.setCurrentStep(VERIFYING);
        utx.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction stx = getServiceHub().signInitialTransaction(utx);

        progressTracker.setCurrentStep(FINALISING);
        FlowSession targetSession = initiateFlow(target);
        return subFlow(new FinalityFlow(stx, ImmutableList.of(targetSession), Objects.requireNonNull(FINALISING.childProgressTracker())));
    }
}
