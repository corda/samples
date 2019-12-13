package net.corda.yo.test.flow;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;

import net.corda.yo.flow.YoFlow;
import net.corda.yo.state.YoState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;


public class YoFlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.yo.flow"),
                TestCordapp.findCordapp("net.corda.yo.contract"),
                TestCordapp.findCordapp("net.corda.yo.state"))));
        a = network.createNode();
        b = network.createNode();
        network.runNetwork();
    }


    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowWorksCorrectly() throws ExecutionException, InterruptedException {
        a.getServices().getMyInfo().getLegalIdentities().get(0);
        YoState yo = new YoState(a.getServices().getMyInfo().getLegalIdentities().get(0), b.getServices().getMyInfo().getLegalIdentities().get(0));
        YoFlow flow = new YoFlow(b.getServices().getMyInfo().getLegalIdentities().get(0));
        CordaFuture future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction stx = (SignedTransaction) future.get();
        SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction((stx).getId());
        assertEquals(bTx, stx);
        System.out.println(bTx.toString() + " == " + stx.toString());

        b.transaction(() -> {
            List<StateAndRef<YoState>> yoStates = b.getServices().getVaultService().queryBy(YoState.class).getStates();
            assertEquals(1, yoStates.size());
            YoState retState = yoStates.get(0).getState().getData();
            assertEquals(retState.toString(), yo.toString());
            System.out.println(retState.toString() + " == " + yo.toString());
            return null;
        });
    }



}
