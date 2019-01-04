package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.examples.obligation.Obligation;
import net.corda.examples.obligation.ObligationContract;
import net.corda.examples.obligation.flows.ObligationBaseFlow.SignTxFlowNoChecking;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static net.corda.examples.obligation.ObligationContract.OBLIGATION_CONTRACT_ID;

public class TransferObligation {
    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends ObligationBaseFlow {
        private final UniqueIdentifier linearId;
        private final Party newLender;
        private final Boolean anonymous;

        private final Step GET_OBLIGATION = new Step("Obtaining obligation from vault.");
        private final Step CHECK_INITIATOR = new Step("Checking current lender is initiating flow.");
        private final Step BUILD_TRANSACTION = new Step("Building and verifying transaction.");
        private final Step SIGN_TRANSACTION = new Step("Signing transaction.");
        private final Step SYNC_OUR_IDENTITY = new Step("Syncing our identity with the counterparties.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return IdentitySyncFlow.Send.Companion.tracker();
            }
        };
        private final Step COLLECT_SIGS = new Step("Collecting counterparty signatures.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step SYNC_OTHER_IDENTITIES = new Step("Making counterparties sync identities with each other.");
        private final Step FINALISE = new Step("Finalising transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(GET_OBLIGATION, CHECK_INITIATOR, BUILD_TRANSACTION, SIGN_TRANSACTION, SYNC_OUR_IDENTITY, COLLECT_SIGS, SYNC_OTHER_IDENTITIES, FINALISE);

        public Initiator(UniqueIdentifier linearId, Party newLender, Boolean anonymous) {
            this.linearId = linearId;
            this.newLender = newLender;
            this.anonymous = anonymous;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Stage 1. Retrieve obligation with the correct linear ID from the vault.
            progressTracker.setCurrentStep(GET_OBLIGATION);
            final StateAndRef<Obligation> obligationToTransfer = getObligationByLinearId(linearId);
            final Obligation inputObligation = obligationToTransfer.getState().getData();

            final Party borrower = getBorrowerIdentity(inputObligation);

            // We call `toSet` in case the borrower and the new lender are the same party.
            FlowSession newLenderSession = initiateFlow(newLender);
            FlowSession borrowerFlowSession = initiateFlow(borrower);
            Set<FlowSession> sessions = ImmutableSet.of(borrowerFlowSession, newLenderSession);

            // Stage 2. This flow can only be initiated by the current lender. Abort if the borrower started this flow.
            progressTracker.setCurrentStep(CHECK_INITIATOR);
            if (!getOurIdentity().equals(getLenderIdentity(inputObligation))) {
                throw new IllegalStateException("Obligation transfer can only be initiated by the lender.");
            }

            // Stage 3. Create the new obligation state reflecting a new lender.
            // This step has to interact with the new lender to exchange identities if we are using anonymous identities.
            progressTracker.setCurrentStep(BUILD_TRANSACTION);
            borrowerFlowSession.send(false); // we don't need to swap identities with the borrower as we'd already have it.
            newLenderSession.send(anonymous);
            final Obligation transferredObligation = createOutputObligation(inputObligation, newLenderSession);

            // Stage 4. Create the transfer command.
            final List<PublicKey> signerKeys = new ImmutableList.Builder<PublicKey>()
                    .addAll(inputObligation.getParticipantKeys())
                    .add(transferredObligation.getLender().getOwningKey()).build();
            final Command transferCommand = new Command<>(new ObligationContract.Commands.Transfer(), signerKeys);

            // Stage 5. Create a transaction builder, add the states and commands, and verify the output.
            final TransactionBuilder builder = new TransactionBuilder(getFirstNotary())
                    .addInputState(obligationToTransfer)
                    .addOutputState(transferredObligation, OBLIGATION_CONTRACT_ID)
                    .addCommand(transferCommand);
            builder.verify(getServiceHub());

            // Stage 6. Sign the transaction using the key we originally used.
            progressTracker.setCurrentStep(SIGN_TRANSACTION);
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder, inputObligation.getLender().getOwningKey());

            // Stage 7. Share our anonymous identity with the borrower and the new lender.
            progressTracker.setCurrentStep(SYNC_OUR_IDENTITY);
            subFlow(new IdentitySyncFlow.Send(sessions, ptx.getTx(), SYNC_OUR_IDENTITY.childProgressTracker()));

            // Stage 8. Collect signatures from the borrower and the new lender.
            progressTracker.setCurrentStep(COLLECT_SIGS);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    sessions,
                    ImmutableList.of(inputObligation.getLender().getOwningKey()),
                    COLLECT_SIGS.childProgressTracker()));

            // Stage 9. Tell the counterparties about each other so they can sync confidential identities.
            progressTracker.setCurrentStep(SYNC_OTHER_IDENTITIES);
            for (FlowSession session: sessions) {
                if (session.getCounterparty().equals(borrower)) session.send(newLender);
                else session.send(borrower);
            }

            // Stage 10. Notarise and record the transaction in our vaults.
            progressTracker.setCurrentStep(FINALISE);
            return subFlow(new FinalityFlow(stx, sessions, FINALISE.childProgressTracker()));
        }

        @Suspendable
        private AbstractParty getLenderIdentity(Obligation inputObligation) {
            if (inputObligation.getLender() instanceof AnonymousParty) {
                return resolveIdentity(inputObligation.getLender());
            } else {
                return inputObligation.getLender();
            }
        }

        @Suspendable
        private Obligation createOutputObligation(Obligation inputObligation, FlowSession newLenderSession) throws FlowException {
            if (anonymous) {
                final SwapIdentitiesFlow.AnonymousResult anonymousIdentitiesResult = subFlow(new SwapIdentitiesFlow(newLenderSession));
                return inputObligation.withNewLender(anonymousIdentitiesResult.getTheirIdentity());
            } else {
                return inputObligation.withNewLender(newLender);
            }
        }

        @Suspendable
        private Party getBorrowerIdentity(Obligation inputObligation) {
            if (inputObligation.getBorrower() instanceof AnonymousParty) {
                return resolveIdentity(inputObligation.getBorrower());
            } else {
                return (Party) inputObligation.getBorrower();
            }
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        private final Step SYNC_FIRST_IDENTITY = new Step("Syncing our identity with the current lender.");
        private final Step SIGN_TRANSACTION = new Step("Signing transaction.");
        private final Step SYNC_SECOND_IDENTITY = new Step("Syncing our identity with the other counterparty.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(SYNC_FIRST_IDENTITY, SIGN_TRANSACTION, SYNC_SECOND_IDENTITY);

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Stage 1. Swap anonymouse identities if needed.
            final boolean exchangeIdentities = otherFlow.receive(Boolean.class).unwrap(data -> data);
            if (exchangeIdentities) {
                subFlow(new SwapIdentitiesFlow(otherFlow));
            }

            // Stage 2. Sync identities with the current lender.
            progressTracker.setCurrentStep(SYNC_FIRST_IDENTITY);
            subFlow(new IdentitySyncFlow.Receive(otherFlow));

            // Stage 3. Sign the transaction.
            progressTracker.setCurrentStep(SIGN_TRANSACTION);
            SignedTransaction stx = subFlow(new SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));

            // Stage 4. Sync identities with the other counterparty.
            progressTracker.setCurrentStep(SYNC_SECOND_IDENTITY);
            Party otherParty = otherFlow.receive(Party.class).unwrap(data -> data);
            subFlow(new IdentitySyncFlowWrapper.Initiator(otherParty, stx.getTx(), SYNC_SECOND_IDENTITY.childProgressTracker()));

            return subFlow(new ReceiveFinalityFlow(otherFlow, stx.getId()));
        }
    }
}
