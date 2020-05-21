package net.corda.examples.yo;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.yo.flows.YoFlow;
import net.corda.examples.yo.flows.YoFlowResponder;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
            TestCordapp.findCordapp("net.corda.examples.yo.contracts"),
            TestCordapp.findCordapp("net.corda.examples.yo.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();

    public FlowTests() {
        ImmutableList.of(a, b).forEach(it -> {
            it.registerInitiatedFlow(YoFlowResponder.class);
        });
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    //The yo flow should not have any input
    //This test will check if the input list is empty
    @Test
    public void dummyTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new YoFlow(b.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        SignedTransaction ptx = future.get();
        assert(ptx.getTx().getInputs().isEmpty());
    }
}
