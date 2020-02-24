package com.heartbeat;

import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode node;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters()
                .withThreadPerNode(true)
                .withCordappsForAllNodes(
                        Collections.singletonList(TestCordapp.findCordapp("com.heartbeat"))
                ));
        node = network.createNode();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void heartbeatOccursEverySecond() throws ExecutionException, InterruptedException {
        StartHeartbeatFlow flow = new StartHeartbeatFlow();
        node.startFlow(flow).get();

        Long sleepTime = Long.valueOf(6000);
        Thread.sleep(sleepTime);

        List<SignedTransaction> recordedTxs = node.transaction(() -> {
            return node.getServices().getValidatedTransactions().track().getSnapshot();
        });

        System.out.println(recordedTxs);

        int totalExpectedTransactions = 7;
        assertEquals(totalExpectedTransactions, recordedTxs.size());
    }

}
