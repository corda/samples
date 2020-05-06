package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.SanctionedEntitiesContract;
import com.example.state.SanctionedEntities;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Collections;

/**
 * This flow allows a party to issue a sanctions list.
 * This sanctions list will be used by other parties when they are making their
 * IOU agreements to determine whether the counter party is trustworthy.
 *
 */
public class IssueSanctionsListFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<StateAndRef<SanctionedEntities>>{
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction");
        ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction."){
          public ProgressTracker childProgressTracker() {
              return FinalityFlow.tracker();
          }
        };
        ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );


        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public  StateAndRef<SanctionedEntities> call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            SanctionedEntities state = new SanctionedEntities(Collections.emptyList(), getServiceHub().getMyInfo().getLegalIdentities().get(0));
            Command txCommand = new Command(new SanctionedEntitiesContract.Commands.Create(), getServiceHub().getMyInfo().getLegalIdentities().get(0).getOwningKey());
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(state, SanctionedEntitiesContract.SANCTIONS_CONTRACT_ID)
                    .addCommand(txCommand);
            txBuilder.verify(getServiceHub());

            // Stage 2
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 3
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(
                    new FinalityFlow(
                            partSignedTx,
                            Collections.emptyList(),
                            FINALISING_TRANSACTION.childProgressTracker()
                    )
            ).getTx().outRefsOfType(SanctionedEntities.class).get(0);
        }
    }
}
