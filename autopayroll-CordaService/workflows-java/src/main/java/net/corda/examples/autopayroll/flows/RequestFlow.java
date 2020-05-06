package net.corda.examples.autopayroll.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.examples.autopayroll.contracts.PaymentRequestContract;
import net.corda.examples.autopayroll.states.PaymentRequestState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// *********
// * Flows *
// *********
public class RequestFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class RequestFlowInitiator extends FlowLogic<SignedTransaction> {

        private final String amount;
        private final Party towhom;

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction between accounts");
        private final Step PROCESSING_TRANSACTION = new Step("Process transaction with our private key");
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.");

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                PROCESSING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        public RequestFlowInitiator(String amount, Party towhom) {
            this.amount = amount;
            this.towhom = towhom;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party bank = getServiceHub().getNetworkMapCache().getPeerByLegalName(new CordaX500Name("BankOperator", "Toronto", "CA"));
            PaymentRequestState output = new PaymentRequestState(amount, towhom, ImmutableList.of(getOurIdentity(), bank));

            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            CommandData commandData = new PaymentRequestContract.Commands.Request();
            txBuilder.addCommand(commandData, getOurIdentity().getOwningKey(), bank.getOwningKey());
            txBuilder.addOutputState(output, PaymentRequestContract.ID);
            txBuilder.verify(getServiceHub());

            progressTracker.setCurrentStep(PROCESSING_TRANSACTION);
            FlowSession session = initiateFlow(bank);
            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, ImmutableList.of(session)));
            return subFlow(new FinalityFlow(stx, ImmutableList.of(session)));
        }
    }

    @InitiatedBy(RequestFlowInitiator.class)
    public static class RequestFlowResponder extends FlowLogic<Void> {

        private final FlowSession counterpartySession;

        public RequestFlowResponder(FlowSession counterPartySession) {
            this.counterpartySession = counterPartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Responder flow logic goes here.
            SignedTransaction stx = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    if (!stx.getInputs().isEmpty()) {
                        throw new FlowException("Payment Request should not have inputs");
                    }
                }
            });

            subFlow(new ReceiveFinalityFlow(counterpartySession, stx.getId()));
            return null;
        }
    }
}
