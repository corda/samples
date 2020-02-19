package com.flowdb;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.flows.FlowLogic;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private final String BITCOIN = "bitcoin";
    private final String E_CASH = "eCash";
    private final Integer INITIAL_BITCOIN_VALUE = 7000;
    private final Integer NEW_BITCOIN_VALUE = 8000;
    private final Integer INITIAL_E_CASH_VALUE = 100;
    private final Integer NEW_E_CASH_VALUE = 200;


    private MockNetwork network;
    private StartedMockNode node;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(
                Collections.singletonList(TestCordapp.findCordapp("com.flowdb"))
        ));
        node = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowWritesToTableCorrectly() throws ExecutionException, InterruptedException {
        FlowLogic<Void> flow1 = new AddTokenValueFlow(BITCOIN, INITIAL_BITCOIN_VALUE);
        CordaFuture<Void> future1 = node.startFlow(flow1);
        network.runNetwork();
        future1.get();

        FlowLogic<Void> flow2 = new UpdateTokenValueFlow(BITCOIN, NEW_BITCOIN_VALUE);
        CordaFuture<Void> future2 = node.startFlow(flow2);
        network.runNetwork();
        future2.get();

        FlowLogic<Integer> flow3 = new QueryTokenValueFlow(BITCOIN);
        CordaFuture<Integer> future3 = node.startFlow(flow3);
        network.runNetwork();
        Integer bitcoinValueFromDB = future3.get();

        assertEquals(NEW_BITCOIN_VALUE, bitcoinValueFromDB);
    }

    @Test
    public void tableSupportsMultipleTokensCorrectly() throws ExecutionException, InterruptedException {
        FlowLogic<Void> flow1 = new AddTokenValueFlow(BITCOIN, INITIAL_BITCOIN_VALUE);
        CordaFuture<Void> future1 = node.startFlow(flow1);
        network.runNetwork();
        future1.get();

        FlowLogic<Void> flow2 = new UpdateTokenValueFlow(BITCOIN, NEW_BITCOIN_VALUE);
        CordaFuture<Void> future2 = node.startFlow(flow2);
        network.runNetwork();
        future2.get();

        FlowLogic<Void> flow3 = new AddTokenValueFlow(E_CASH, INITIAL_E_CASH_VALUE);
        CordaFuture<Void> future3 = node.startFlow(flow3);
        network.runNetwork();
        future1.get();

        FlowLogic<Void> flow4 = new UpdateTokenValueFlow(E_CASH, NEW_E_CASH_VALUE);
        CordaFuture<Void> future4 = node.startFlow(flow4);
        network.runNetwork();
        future2.get();

        FlowLogic<Integer> flow5 = new QueryTokenValueFlow(BITCOIN);
        CordaFuture<Integer> future5 = node.startFlow(flow5);
        network.runNetwork();
        Integer bitcoinValueFromDB = future5.get();

        FlowLogic<Integer> flow6 = new QueryTokenValueFlow(E_CASH);
        CordaFuture<Integer> future6 = node.startFlow(flow6);
        network.runNetwork();
        Integer eCashValueFromDB = future6.get();

        assertEquals(NEW_BITCOIN_VALUE, bitcoinValueFromDB);
        assertEquals(NEW_E_CASH_VALUE, eCashValueFromDB);

    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void errorIsThrownIfTokenNotInTable() throws ExecutionException, InterruptedException {
        thrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));

        FlowLogic<Integer> flow = new QueryTokenValueFlow(BITCOIN);
        CordaFuture<Integer> future = node.startFlow(flow);
        network.runNetwork();
        future.get();
    }

}
