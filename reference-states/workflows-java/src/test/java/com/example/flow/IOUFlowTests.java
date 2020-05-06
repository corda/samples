package com.example.flow;

import com.example.contract.SanctionedEntitiesContract;
import com.example.state.SanctionableIOUState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.NotaryException;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static net.corda.testing.core.TestConstants.DUMMY_NOTARY_NAME;
import static net.corda.testing.node.internal.InternalTestUtilsKt.findCordapp;

public class IOUFlowTests {

    MockNetwork network;
    StartedMockNode a;
    StartedMockNode b;
    StartedMockNode c;
    StartedMockNode issuer;
    Party issuerParty;

    @Before
    public void setup(){
        MockNetworkParameters param = new MockNetworkParameters(
                false,
                false,
                new InMemoryMessagingNetwork.ServicePeerAllocationStrategy.Random(),
                ImmutableList.of(new MockNetworkNotarySpec(DUMMY_NOTARY_NAME,false)),
                new NetworkParameters(4,emptyList(),10484760, 10484760 * 50, Instant.now(), 1, emptyMap(), Duration.ofDays(30)),
                 ImmutableList.of(findCordapp("com.example.contract"))
        );

        network = new MockNetwork(param);

        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);

        issuer = network.createPartyNode(null);
        issuerParty = issuer.getInfo().getLegalIdentities().get(0);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.

        ImmutableList.of(a,b,c ).forEach(node->{
            node.registerInitiatedFlow(IOUIssueFlow.Acceptor.class);
        });
        issuer.registerInitiatedFlow(GetSanctionsListFlow.Acceptor.class);
        network.runNetwork();

    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Test
    public void dealFailsIfThereIsNoIssuedSanctionsList() throws ExecutionException, InterruptedException, SignatureException {
        thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(TransactionVerificationException.class));
        IOUIssueFlow.Initiator flow = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = (SignedTransaction) future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());
        System.out.println(signedTx.getCoreTransaction().outputsOfType(SanctionableIOUState.class).get(0));
    }

    @Test
    public void dealSucceedsWithIssuedSanctions() throws ExecutionException, InterruptedException, SignatureException {
        Future issuanceFlow = issuer.startFlow(new IssueSanctionsListFlow.Initiator());
        network.runNetwork();
        issuanceFlow.get();
        getSanctionsList(a, issuerParty);
        IOUIssueFlow.Initiator flow = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future future  =  a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = (SignedTransaction) future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());

    }


    @Test
    public void dealIsRejectedIfPartyIsSanctioned() throws ExecutionException, InterruptedException {
        thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(TransactionVerificationException.class));
        Future issuanceFlow = issuer.startFlow(new IssueSanctionsListFlow.Initiator());
        network.runNetwork();
        issuanceFlow.get();

        Future updateFuture = issuer.startFlow(new UpdateSanctionsListFlow.Initiator(b.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        updateFuture.get();


        IOUIssueFlow.Initiator flow = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future future = a.startFlow(flow);
        network.runNetwork();

        future.get();
    }

    @Test
    public void dealFailsIfListIsUpdatedAfterCollection() throws ExecutionException, InterruptedException, SignatureException {
        thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(NotaryException.class));
        Future issuanceFlow = issuer.startFlow(new IssueSanctionsListFlow.Initiator());
        network.runNetwork();
        issuanceFlow.get();
        getSanctionsList(a, issuerParty);

        IOUIssueFlow.Initiator iouIssueFlow = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouIssueFuture = a.startFlow(iouIssueFlow);
        network.runNetwork();

        SignedTransaction iouIssueTransaction = (SignedTransaction) iouIssueFuture.get();
        iouIssueTransaction.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());

        Future updateIOUFuture = issuer.startFlow(new UpdateSanctionsListFlow.Initiator(c.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        updateIOUFuture.get();

        IOUIssueFlow.Initiator iouReissue = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouReissueFuture = a.startFlow(iouReissue);
        network.runNetwork();

        iouReissueFuture.get();
    }

    @Test
    public void dealSucceedsIfListIsCollectedAgainAfterUpdate() throws ExecutionException, InterruptedException, SignatureException {
        Future issuanceFlow = issuer.startFlow(new IssueSanctionsListFlow.Initiator());
        network.runNetwork();
        issuanceFlow.get();
        getSanctionsList(a, issuerParty);

        IOUIssueFlow.Initiator iouIssueFlow = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouIssueFuture = a.startFlow(iouIssueFlow);
        network.runNetwork();

        SignedTransaction iouIssueTransaction = (SignedTransaction) iouIssueFuture.get();
        iouIssueTransaction.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());

        Future updateIOUFuture = issuer.startFlow(new UpdateSanctionsListFlow.Initiator(c.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        updateIOUFuture.get();

        //update on node a only
        Future getUpdatedListAgain = a.startFlow(new GetSanctionsListFlow.Initiator(issuerParty));
        network.runNetwork();
        getUpdatedListAgain.get();

        IOUIssueFlow.Initiator iouDeal2 = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouDeal2Future = a.startFlow(iouDeal2);
        network.runNetwork();
        iouDeal2Future.get();

        IOUIssueFlow.Initiator iouDeal3 = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouDeal3Future = a.startFlow(iouDeal3);
        network.runNetwork();

        iouDeal3Future.get();
    }

    @Test
    public void duringTxResolutionLatestRefIsProvidedToCounterparty() throws ExecutionException, InterruptedException, SignatureException {
        Future issuanceFlow = issuer.startFlow(new IssueSanctionsListFlow.Initiator());
        network.runNetwork();
        issuanceFlow.get();
        getSanctionsList(a, issuerParty);

        IOUIssueFlow.Initiator iouIssueFlow = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouIssueFuture = a.startFlow(iouIssueFlow);
        network.runNetwork();

        SignedTransaction iouIssueTransaction = (SignedTransaction) iouIssueFuture.get();
        iouIssueTransaction.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());

        Future updateIOUFuture = issuer.startFlow(new UpdateSanctionsListFlow.Initiator(c.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        updateIOUFuture.get();

        //update on node a only
        Future getUpdatedListAgain = a.startFlow(new GetSanctionsListFlow.Initiator(issuerParty));
        network.runNetwork();
        getUpdatedListAgain.get();

        //take down issuer, so isn't able to provide new list to node b
        issuer.stop();

        IOUIssueFlow.Initiator iouDeal2 = new IOUIssueFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0), issuerParty);
        Future iouDeal2Future = a.startFlow(iouDeal2);
        network.runNetwork();
        iouDeal2Future.get();


    }


    private void getSanctionsList(StartedMockNode node, Party issuerOfSanctions) throws ExecutionException, InterruptedException {
        Future flow = node.startFlow(new GetSanctionsListFlow.Initiator(issuerOfSanctions));
        network.runNetwork();
        flow.get();


    }




}
