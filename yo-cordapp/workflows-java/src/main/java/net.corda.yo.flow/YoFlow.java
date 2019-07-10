package net.corda.yo.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.yo.contract.YoContract;
import net.corda.yo.state.YoState;

import java.security.SignatureException;


@InitiatingFlow
@StartableByRPC
public class YoFlow extends FlowLogic<SignedTransaction> {
    private final Step CREATING = new Step("Creating a new Yo!");
    private final Step SIGNING = new Step("Signing the Yo!");
    private final Step VERIFYING = new Step("Verifying the Yo!");
    private final Step FINALISING = new Step("Sending the Yo!") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };
    private final Party target;
    private final ProgressTracker progressTracker = new ProgressTracker(
            CREATING,
            SIGNING,
            VERIFYING,
            FINALISING
    );

    public YoFlow(Party target){
        this.target = target;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        progressTracker.setCurrentStep(CREATING);
        ServiceHub serviceHub = getServiceHub();
        Party me = serviceHub.getMyInfo().getLegalIdentities().get(0);
        Party notary = serviceHub.getNetworkMapCache().getNotaryIdentities().get(0);
        Command command = new Command<>( new YoContract.Commands.Send(),
                ImmutableList.of(me.getOwningKey()));
        YoState state = new YoState(me,target,"Yo");
        StateAndContract stateAndContract = new StateAndContract(state, YoContract.ID);
        TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract,command);

        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction stx = serviceHub.signInitialTransaction(utx);

        progressTracker.setCurrentStep(VERIFYING);
        try {
            stx.verify(serviceHub);
        } catch (SignatureException e) {
            throw new FlowException();
        }

        progressTracker.setCurrentStep(FINALISING);
        FlowSession targetSession = initiateFlow(target);
        return subFlow(new FinalityFlow(stx, ImmutableSet.of(targetSession), FINALISING.childProgressTracker()));



    }
}


