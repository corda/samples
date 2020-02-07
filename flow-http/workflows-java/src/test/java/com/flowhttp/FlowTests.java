package com.flowhttp;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.flows.FlowLogic;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode node;

    @Before
    public void Setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(
                Collections.singletonList(TestCordapp.findCordapp("com.flowhttp"))
        ));
        node = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void testFlowReturnsCorrectHtml() throws ExecutionException, InterruptedException, IOException {
        // The flow should return the first commit of the BitCoin readme.
        FlowLogic<String> flow = new HttpCallFlow();
        CordaFuture<String> future = node.startFlow(flow);
        network.runNetwork();
        String returnValue = future.get();

        // We run the flow and retrieve its return value.
        Request httpRequest = new Request.Builder().url(Constants.BITCOIN_README_URL).build();
        Response httpResponse = new OkHttpClient().newCall(httpRequest).execute();
        String expectedValue = httpResponse.body().string();

        // We check that the strings are equal.
        assertEquals(expectedValue, returnValue);
    }
}
