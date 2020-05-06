package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import kotlin.collections.SetsKt;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;

public class IdentitySyncFlowWrapper {
    @InitiatingFlow
    public static class Initiator extends FlowLogic<Boolean> {
        private final Party otherParty;
        private final WireTransaction tx;

        private final Step SYNCING_WRAPPER = new Step("Syncing Wrapper") {
            @Override
            public ProgressTracker childProgressTracker() { return IdentitySyncFlow.Send.Companion.tracker(); }
        };
        private ProgressTracker progressTracker = new ProgressTracker(SYNCING_WRAPPER);

        public Initiator(Party otherParty, WireTransaction tx, ProgressTracker progressTracker) {
            this.otherParty = otherParty;
            this.tx = tx;
            this.progressTracker = progressTracker;
        }
        public Initiator(Party otherParty, WireTransaction tx) {
            this.otherParty = otherParty;
            this.tx = tx;
        }
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public Boolean call() throws FlowException {
            final FlowSession otherSession = initiateFlow(otherParty);
            subFlow(new IdentitySyncFlow.Send(SetsKt.setOf(otherSession), tx, SYNCING_WRAPPER.childProgressTracker()));
            return otherSession.receive(Boolean.class).unwrap(it -> it);
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Receive extends FlowLogic<Void> {
        private final FlowSession otherSideSession;
        public Receive(FlowSession otherSideSession) {
            this.otherSideSession = otherSideSession;
        }
        @Suspendable
        @Override
        public Void call() throws FlowException {
            subFlow(new IdentitySyncFlow.Receive(otherSideSession));
            otherSideSession.send(true);
            return null;
        }
    }
}
