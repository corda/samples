package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.SanctionableIOUContract;
import com.example.state.SanctionableIOUState;
import com.example.state.SanctionedEntities;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.ReferencedStateAndRef;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.example.contract.SanctionableIOUContract.IOU_CONTRACT_ID;


public class IOUIssueFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private int iouValue;
        private Party otherParty;
        private Party sanctionsBody;

        public Initiator(int iouValue, Party otherParty, Party sanctionsBody) {
            this.iouValue = iouValue;
            this.otherParty = otherParty;
            this.sanctionsBody = sanctionsBody;
        }

        ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating Transaction based on new IOU.");
        ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction.") {
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            StateAndRef<SanctionedEntities> sanctionsListToUse = getSanctionsList(sanctionsBody);
            SanctionableIOUState iouState = new SanctionableIOUState(iouValue, getServiceHub().getMyInfo().getLegalIdentities().get(0), otherParty);

            Command txCommand = new Command(
                    new SanctionableIOUContract.Commands.Create(sanctionsBody),
                    iouState.getParticipants().stream().map(it -> it.getOwningKey()).collect(Collectors.toList())
            );

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(iouState, IOU_CONTRACT_ID)
                    .addCommand(txCommand);

            if (sanctionsListToUse != null) {
                txBuilder.addReferenceState(new ReferencedStateAndRef<>(sanctionsListToUse));
            }
            ;

            //Stage 2
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.

            txBuilder.verify(getServiceHub());

            //Stage 3
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            //Sign the transaction
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4
            progressTracker.setCurrentStep(GATHERING_SIGS);
            //Send the state to the counterparty, and receive it back with their signature
            FlowSession otherPartySession = initiateFlow(otherParty);
            SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(
                            partSignedTx,
                            ImmutableSet.of(otherPartySession),
                            GATHERING_SIGS.childProgressTracker()
                    )
            );

            // Stage 5
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(
                    new FinalityFlow(
                            fullySignedTx,
                            ImmutableSet.of(otherPartySession),
                            FINALISING_TRANSACTION.childProgressTracker()
                    )
            );

        }


        @Suspendable
        public StateAndRef<SanctionedEntities> getSanctionsList(Party sanctionsBody) {
            Predicate<StateAndRef<SanctionedEntities>> byIssuer = sanctionEnt -> (sanctionEnt.getState().getData()).getIssuer().equals(sanctionsBody);
            List<StateAndRef<SanctionedEntities>> sanctionLists = getServiceHub().getVaultService().queryBy(SanctionedEntities.class).getStates().stream().filter(byIssuer).collect(Collectors.toList());
            if(sanctionLists.isEmpty()){
                return null;
            }else {
                return sanctionLists.get(0);
            }
        }
    }


    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {
        private FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            SignTransactionFlow signTransactionFlow = new SignTransactionFlow(otherPartySession) {
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    if (!(output instanceof SanctionableIOUState)) {
                        throw new FlowException("This must be an invoice transaction");
                    }
                    SanctionableIOUState iou = (SanctionableIOUState) output;
                    if (!(iou.getValue() <= 100)) {
                        throw new FlowException("I won't accept IOUs with a value over 100.");
                    }
                }

            };
            SecureHash txId = subFlow(signTransactionFlow).getId();
            SignedTransaction recordedTx = subFlow(
                    new ReceiveFinalityFlow(
                            otherPartySession,
                            txId,
                            StatesToRecord.ALL_VISIBLE
                    )
            );
            return recordedTx;
        }
    }
}

