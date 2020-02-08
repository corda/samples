package net.corda.examples.oracle.client;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.oracle.base.contract.PrimeState;
import net.corda.examples.oracle.client.flow.CreatePrime;
import net.corda.examples.oracle.service.flow.QueryHandler;
import net.corda.examples.oracle.service.flow.SignHandler;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class PrimeClientTests {
    private final MockNetwork mockNet = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(
            ImmutableList.of(
                TestCordapp.findCordapp("net.corda.examples.oracle.service.service"),
                TestCordapp.findCordapp("net.corda.examples.oracle.base.contract")
            ))
    );
    private final StartedMockNode a = mockNet.createNode();

    @Before
    public void setup() {
        CordaX500Name oracleName = new CordaX500Name("Oracle", "New York", "US");
        StartedMockNode oracle = mockNet.createNode(new MockNodeParameters().withLegalName(oracleName));
        ImmutableList.of(QueryHandler.class, SignHandler.class).forEach(oracle::registerInitiatedFlow);

        mockNet.runNetwork();
    }

    @After
    public void tearDown() {
        mockNet.stopNodes();
    }

    @Test
    public void oracleReturnsCorrectNthPrime() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> flow = a.startFlow(new CreatePrime(100));
        mockNet.runNetwork();
        PrimeState result = flow.get().getTx().outputsOfType(PrimeState.class).get(0);

        assertEquals(100, result.getN().longValue());
        int prime100 = 541;
        assertEquals(prime100, result.getNthPrime().longValue());
    }
}
