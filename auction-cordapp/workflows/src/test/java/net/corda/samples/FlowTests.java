package net.corda.samples;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.flows.BidFlow;
import net.corda.samples.flows.CreateAssetFlow;
import net.corda.samples.flows.CreateAuctionFlow;
import net.corda.samples.states.Asset;
import net.corda.samples.states.AuctionState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.Assert.*;

public class FlowTests {
    private  MockNetwork network;
    private  StartedMockNode a;
    private  StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        ImmutableList.of(
                            TestCordapp.findCordapp("net.corda.samples.flows"),
                            TestCordapp.findCordapp("net.corda.samples.contracts")
                        )
                )
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void testCreateAssetFlow() throws Exception {
        CreateAssetFlow flow = new CreateAssetFlow("Test Asset", "Dummy Asser", "http://abc.com/dummy.png");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        SignedTransaction signedTransaction = future.get();
        Asset asset = (Asset) signedTransaction.getTx().getOutput(0);
        assertNotNull(asset);
    }

    @Test
    public void testCreateAuctionFlow() throws Exception {
        CreateAssetFlow assetflow = new CreateAssetFlow("Test Asset", "Dummy Asser", "http://abc.com/dummy.png");
        CordaFuture<SignedTransaction> future = a.startFlow(assetflow);
        SignedTransaction signedTransaction = future.get();
        Asset asset = (Asset) signedTransaction.getTx().getOutput(0);
        CreateAuctionFlow.Initiator auctionFlow = new CreateAuctionFlow.Initiator(Amount.parseCurrency("1000 USD"),
                asset.getLinearId().getId(), LocalDateTime.ofInstant(Instant.now().plusMillis(30000), ZoneId.systemDefault()));
        CordaFuture<SignedTransaction> future1 = a.startFlow(auctionFlow);
        SignedTransaction transaction = future1.get();
        AuctionState auctionState = (AuctionState) transaction.getTx().getOutput(0);
        assertNotNull(auctionFlow);
    }
}