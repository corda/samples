package net.corda.examples.carinsurance;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.carinsurance.flows.InsuranceInfo;
import net.corda.examples.carinsurance.flows.IssueInsuranceFlow;
import net.corda.examples.carinsurance.flows.VehicleInfo;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
        TestCordapp.findCordapp("net.corda.examples.carinsurance.contracts"),
        TestCordapp.findCordapp("net.corda.examples.carinsurance.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();


    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    //simple example test to test if the issue insurance flow only carries one output.
    @Test
    public void dummyTest() throws Exception{
        VehicleInfo car = new VehicleInfo(
                "I4U64FY56I48Y",
                "165421658465465",
                "BMW",
                "M3",
                "MPower",
                "Black",
                "gas");
        InsuranceInfo policy1 = new InsuranceInfo(
                "8742",
                2000,
                18,
                49,
                car);

        IssueInsuranceFlow.IssueInsuranceInitiator flow = new IssueInsuranceFlow.IssueInsuranceInitiator(policy1,b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction ptx = future.get();

        //assertion for single output
        assertEquals(1, ptx.getTx().getOutputStates().size());

    }
}
