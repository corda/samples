package com.example.test.flow;


import com.google.common.collect.ImmutableList;
import com.example.flow.IssueInvoiceFlow;
import com.example.flow.QueryRateHandler;
import com.example.state.InvoiceState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.SignatureException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class IssueInvoiceFlowTests {
    private MockNetwork network;
    private StartedMockNode contractor;
    private StartedMockNode megaCorp;
    private StartedMockNode oracle;
    private LocalDate today;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters(
                ImmutableList.of(
                        TestCordapp.findCordapp("com.example.contract"),
                        TestCordapp.findCordapp("com.example.flow"),
                        TestCordapp.findCordapp("com.example.state"),
                        TestCordapp.findCordapp("com.example.service")
                )
        ));
        CordaX500Name oracleName = new CordaX500Name("Oracle", "London", "GB");
        contractor = network.createPartyNode(null);
        megaCorp = network.createPartyNode(null);
        oracle = network.createPartyNode(oracleName);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        ImmutableList.of(contractor, megaCorp).forEach(it -> {
            it.registerInitiatedFlow(IssueInvoiceFlow.Acceptor.class);
            it.registerInitiatedFlow(QueryRateHandler.class);
        });
        network.runNetwork();
        today = LocalDate.now();
    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    @Test(expected = ExecutionException.class)
    public void flowRejectsInvalidInvoices() throws ExecutionException, InterruptedException {
        IssueInvoiceFlow.Initiator flow = new IssueInvoiceFlow.Initiator(-1, today, megaCorp.getInfo().getLegalIdentities().get(0));
        Future future = contractor.startFlow(flow);

        network.runNetwork();
        future.get();
    }

    @Test
    public void SignedTransactionsReturnedByFlowIsSignedByInitiator() throws ExecutionException, InterruptedException, SignatureException {
        IssueInvoiceFlow.Initiator flow = new IssueInvoiceFlow.Initiator(1, today, megaCorp.getInfo().getLegalIdentities().get(0));
        Future future = contractor.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();
        stx.verifySignaturesExcept(megaCorp.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void SignedTRansactionReturnedByFlowIsSignedByAcceptor() throws SignatureException, ExecutionException, InterruptedException {
        IssueInvoiceFlow.Initiator flow = new IssueInvoiceFlow.Initiator(1, today, megaCorp.getInfo().getLegalIdentities().get(0));
        Future future = contractor.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();
        stx.verifySignaturesExcept(contractor.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void FlowRecordsATransationInBothPartiesTransactioStorage() throws ExecutionException, InterruptedException {
        IssueInvoiceFlow.Initiator flow = new IssueInvoiceFlow.Initiator(1, today, megaCorp.getInfo().getLegalIdentities().get(0));
        Future future = contractor.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();

        for (StartedMockNode node: ImmutableList.of(contractor, megaCorp)
             ) {
            Assert.assertEquals(stx, node.getServices().getValidatedTransactions().getTransaction(stx.getId()));

        }
    }

    @Test
    public void recordedTransactionHAsNoIputsAndASingleOutput() throws ExecutionException, InterruptedException {
        int invoiceValue = 1;
        IssueInvoiceFlow.Initiator flow = new IssueInvoiceFlow.Initiator(invoiceValue, today, megaCorp.getInfo().getLegalIdentities().get(0));
        Future future = contractor.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();

        for (StartedMockNode node: ImmutableList.of(contractor, megaCorp)
        ) {
            SignedTransaction rtx = node.getServices().getValidatedTransactions().getTransaction(stx.getId());
            List<TransactionState<ContractState>> txOutputs = rtx.getTx().getOutputs();
            assert(txOutputs.size() == 1);

            InvoiceState recordedState = (InvoiceState) txOutputs.get(0).getData();

            Assert.assertEquals(recordedState.getHoursWorked(), invoiceValue);
            Assert.assertEquals(recordedState.getContractor(), contractor.getInfo().getLegalIdentities().get(0));
            Assert.assertEquals(recordedState.getCompany(), megaCorp.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheCorrectInvoicesInBothPartiesVaults() throws ExecutionException, InterruptedException {
        int invoiceValue = 1;
        IssueInvoiceFlow.Initiator flow = new IssueInvoiceFlow.Initiator(invoiceValue, today, megaCorp.getInfo().getLegalIdentities().get(0));
        Future future = contractor.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();

        for (StartedMockNode node: ImmutableList.of(contractor, megaCorp)
        ) {
            node.transaction(() -> {
                   List<StateAndRef<InvoiceState>>  invoices = node.getServices().getVaultService().queryBy(InvoiceState.class).getStates();
                   Assert.assertEquals(1, invoices.size());
                   InvoiceState recordedState = invoices.get(0).getState().getData();
                   Assert.assertEquals(recordedState.getHoursWorked(), invoiceValue);
                   Assert.assertEquals(recordedState.getDate(), today);
                   Assert.assertEquals(recordedState.getContractor(), contractor.getInfo().getLegalIdentities().get(0));
                   Assert.assertEquals(recordedState.getCompany(), megaCorp.getInfo().getLegalIdentities().get(0));
                   return null;
            });

        }
    }
}

