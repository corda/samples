package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import com.example.contract.InvoiceContract;
import com.example.service.Rate;
import com.example.service.RateOf;
import com.example.state.InvoiceState;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the Invoice encapsulated
 * within an [InvoiceState].
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * Sample call:
 *   start IssueInvoiceFlow hoursWorked: 8, date: 2019-05-20, otherParty: "O=MegaCorp 1,L=New York,C=US"
 */
public class IssueInvoiceFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private int hoursWorked;
        private LocalDate date;
        private Party otherParty;

        public Initiator(int hoursWorked, LocalDate date, Party otherParty) {
            this.hoursWorked = hoursWorked;
            this.date = date;
            this.otherParty = otherParty;
        }

        ProgressTracker.Step DETERMINING_SALARY = new ProgressTracker.Step("Determining salary rate for contractor/company.");
        ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new hours submission.");
        ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        ProgressTracker.Step ORACLE_SIGNS = new ProgressTracker.Step("Requesting oracle signature.");
        ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature."){
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Determining salary rate for contractorompany."){
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                DETERMINING_SALARY,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                ORACLE_SIGNS,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(DETERMINING_SALARY);
            // Query the SalaryRateOracle for the billable rate
            CordaX500Name oracleName = new CordaX500Name("Oracle", "London","GB");
            Party oracle = getServiceHub().getNetworkMapCache().getNodeByLegalName(oracleName).getLegalIdentities().get(0);

            Rate rate = subFlow(new QueryRate(new RateOf(getServiceHub().getMyInfo().getLegalIdentities().get(0), otherParty), oracle));

            // Stage 2.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party contractor = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            InvoiceState invoiceState = new InvoiceState(date,hoursWorked,rate.getVal(), contractor, otherParty,
                    oracle, false);
            List<PublicKey> commandSigners = new ArrayList<>();
            for (AbstractParty participant: invoiceState.getParticipants()
                 ) {
                commandSigners.add(participant.getOwningKey());
            }
            commandSigners.add(oracle.getOwningKey());
            Command txCommand = new Command(new InvoiceContract.Commands.Create(contractor,otherParty, rate.getVal()), commandSigners);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(invoiceState, InvoiceContract.ID)
                    .addCommand(txCommand);

            // Stage 3
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());


            // Stage 4
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 5
            progressTracker.setCurrentStep(ORACLE_SIGNS);
            //Have the oracle sign the transaction
            Predicate predicate = new Predicate() {
                @Override
                public boolean test(Object o) {
                    if(o instanceof Command){
                       if(((Command) o).getSigners().contains(oracle.getOwningKey()) && ((Command) o).getValue() instanceof InvoiceContract.Commands.Create){
                           return true;
                       }
                    }
                    return false;
                }
            };

            FilteredTransaction ftx = partSignedTx.buildFilteredTransaction(predicate);

            partSignedTx.withAdditionalSignature(subFlow(new SignRate(txBuilder, oracle, ftx)));

            // Stage 6
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(otherParty);
            FlowSession oracleSession = initiateFlow(oracle);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession,oracleSession), GATHERING_SIGS.childProgressTracker()));

            // Stage 7
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in all parties' vaults.


            return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(otherPartySession, oracleSession), FINALISING_TRANSACTION.childProgressTracker()));
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
            SignTransactionFlow signTransactionFlow = new SignTransactionFlow(otherPartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    if(!(output instanceof InvoiceState )){
                        throw new FlowException("This must be an invoice transaction");
                    }
                    InvoiceState invoice = (InvoiceState) output;
                    if(!(invoice.getHoursWorked() <= 10)){
                        throw new FlowException("Invoices with a value over 10 aren't accepted");
                    }
                }

            };
            SecureHash txId = subFlow(signTransactionFlow).getId();
            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }

}
