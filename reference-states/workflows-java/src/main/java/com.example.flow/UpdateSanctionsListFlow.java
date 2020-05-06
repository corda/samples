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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdateSanctionsListFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<StateAndRef<SanctionedEntities>>{
        Party partyToSanction;

        public Initiator(Party partyToSanction) {
            this.partyToSanction = partyToSanction;
        }

        ProgressTracker.Step ADDING_PARTY_TO_LIST = new ProgressTracker.Step("Sanctioning Party: ");
        ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction");
        ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction."){
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };
        ProgressTracker progressTracker = new ProgressTracker(
                ADDING_PARTY_TO_LIST,
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        @Suspendable
        @Override
        public StateAndRef<SanctionedEntities> call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            StateAndRef<SanctionedEntities> oldList  = getServiceHub().getVaultService().queryBy(SanctionedEntities.class).getStates().get(0);
            SanctionedEntities oldStateData = oldList.getState().getData();
            List<Party> badPeople = new ArrayList<>(oldStateData.getBadPeople());
            badPeople.add(partyToSanction);
            SanctionedEntities newList = new SanctionedEntities(
                    badPeople,
                    oldStateData.getIssuer(),
                    oldStateData.getLinearId()
            );

            // Stage 1
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            //Create an unsigned transaction
            Command txCommand = new Command(new SanctionedEntitiesContract.Commands.Update(), getServiceHub().getMyInfo().getLegalIdentities().get(0).getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(newList, SanctionedEntitiesContract.SANCTIONS_CONTRACT_ID)
                    .addInputState(oldList)
                    .addCommand(txCommand);

            // Stage 2
            progressTracker.setCurrentStep(ADDING_PARTY_TO_LIST);
            txBuilder.verify(getServiceHub());

            // Stage 3
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Notarise and record the transaction in both parties' vaults.
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
