package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
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
import java.time.Duration;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;

public class IssueObligation {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends ObligationBaseFlow {
        private final Amount<Currency> amount;
        private final Party lender;
        private final Boolean anonymous;

        private final Step INITIALISING = new Step("Performing initial steps.");
        private final Step BUILDING = new Step("Performing initial steps.");
        private final Step SIGNING = new Step("Signing transaction.");
        private final Step COLLECTING = new Step("Collecting counterparty signature.") {
            @Override public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING = new Step("Finalising transaction.") {
            @Override public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING
        );

        public Initiator(Amount<Currency> amount, Party lender, Boolean anonymous) {
            this.amount = amount;
            this.lender = lender;
            this.anonymous = anonymous;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Step 1. Initialisation.
            progressTracker.setCurrentStep(INITIALISING);
            final FlowSession lenderFlow = initiateFlow(lender);
            lenderFlow.send(anonymous);
            final Obligation obligation = createObligation(lenderFlow);
            final PublicKey ourSigningKey = obligation.getBorrower().getOwningKey();

            // Step 2. Building.
            progressTracker.setCurrentStep(BUILDING);
            final List<PublicKey> requiredSigners = obligation.getParticipantKeys();

            final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())
                    .addOutputState(obligation, ObligationContract.OBLIGATION_CONTRACT_ID)
                    .addCommand(new ObligationContract.Commands.Issue(), requiredSigners)
                    .setTimeWindow(getServiceHub().getClock().instant(), Duration.ofMinutes(5));

            // Step 3. Sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, ourSigningKey);

            // Step 4. Get the counter-party signature.
            progressTracker.setCurrentStep(COLLECTING);
            final ImmutableSet<FlowSession> sessions = ImmutableSet.of(lenderFlow);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    sessions,
                    ImmutableList.of(ourSigningKey),
                    COLLECTING.childProgressTracker())
            );

            // Step 5. Finalise the transaction.
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, sessions, FINALISING.childProgressTracker()));
        }

        @Suspendable
        private Obligation createObligation(FlowSession lenderSession) throws FlowException {
            if (anonymous) {
                final LinkedHashMap<Party, AnonymousParty> anonymousIdentities = subFlow(new SwapIdentitiesFlow(lenderSession));
                return new Obligation(amount, anonymousIdentities.get(lenderSession.getCounterparty()), anonymousIdentities.get(getOurIdentity()));
            } else {
                return new Obligation(amount, lender, getOurIdentity());
            }
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final boolean anonymous = otherFlow.receive(Boolean.class).unwrap(data -> data);
            if (anonymous) {
                subFlow(new SwapIdentitiesFlow(otherFlow, SwapIdentitiesFlow.tracker()));
            }
            final SignedTransaction stx = subFlow(new SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return subFlow(new ReceiveFinalityFlow(otherFlow, stx.getId()));
        }
    }
}