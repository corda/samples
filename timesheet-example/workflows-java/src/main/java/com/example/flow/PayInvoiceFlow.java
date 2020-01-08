package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.example.contract.InvoiceContract;
import com.example.state.InvoiceState;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;

import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.finance.workflows.asset.CashUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Currency;
import java.util.UUID;

import static net.corda.finance.Currencies.POUNDS;

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [InvoiceState].
 *
 * In our simple timesheet, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class PayInvoiceFlow {

 @InitiatingFlow
 @StartableByRPC
 public static class Initiator extends FlowLogic<SignedTransaction>{
     private UUID invoiceId;
     private ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new hours submission.");
     private ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
     private ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
     private ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction."){
         @Override
         public ProgressTracker childProgressTracker(){
             return FinalityFlow.tracker();
         }
     };

     public Initiator(UUID invoiceId) {
         this.invoiceId = invoiceId;
     }

     private final ProgressTracker progressTracker = new ProgressTracker(
             GENERATING_TRANSACTION,
             VERIFYING_TRANSACTION,
             SIGNING_TRANSACTION,
             FINALISING_TRANSACTION
     );

     @Nullable
     @Override
     public ProgressTracker getProgressTracker() {
         return progressTracker;
     }

     /**
      * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
      * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
      */
     @Override
     @Suspendable
     public SignedTransaction call() throws FlowException {
         // Obtain a reference to the notary we want to use.

         Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
         StateAndRef invoiceAndRef = getServiceHub().getVaultService().queryBy(InvoiceState.class, new QueryCriteria.LinearStateQueryCriteria().withUuid(ImmutableList.of(invoiceId))).getStates().get(0);
         InvoiceState invoice = (InvoiceState) invoiceAndRef.getState().getData();
         Amount<Currency> paymentAmount = POUNDS ((invoice.getHoursWorked() * invoice.getRate()));
         // We're MegaCorp.  Let's print some money.

         subFlow(new CashIssueFlow(paymentAmount, OpaqueBytes.of((byte) 1),notary));

         // Stage 1.
         progressTracker.setCurrentStep(GENERATING_TRANSACTION);
         // Generate an unsigned transaction.
         Command txCommand = new Command(new InvoiceContract.Commands.Pay(), getServiceHub().getMyInfo().getLegalIdentities().get(0).getOwningKey());
         TransactionBuilder txBuilder = new TransactionBuilder(notary)
                 .addInputState(invoiceAndRef)
                 .addOutputState(new InvoiceState(invoice.getDate(), invoice.getHoursWorked(), invoice.getRate(), invoice.getContractor(), invoice.getCompany(), invoice.getOracle(), true))
                 .addCommand(txCommand);
         // Add our payment to the contractor
         CashUtils.generateSpend(getServiceHub(), txBuilder, paymentAmount, getServiceHub().getMyInfo().getLegalIdentitiesAndCerts().get(0),invoice.getContractor(), ImmutableSet.of());

         // Stage 2.
         progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
         // Verify that the transaction is valid.
         txBuilder.verify(getServiceHub());

         // Stage 3.
         progressTracker.setCurrentStep(SIGNING_TRANSACTION);
         // Sign the transaction.
         SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

         // Stage 4.
         FlowSession contractorSession = initiateFlow(invoice.getContractor());
         progressTracker.setCurrentStep(FINALISING_TRANSACTION);
         // Notarise and record the transaction in all parties' vaults.
         return subFlow(new FinalityFlow(signedTx, contractorSession));
     }
 }

    @InitiatingFlow
    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction>{
        FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }

}
