package bootcamp;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        Collections.singletonList(TestCordapp.findCordapp("bootcamp"))
                )
        );
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

//    @Test
//    public void transactionConstructedByFlowUsesTheCorrectNotary() throws Exception {
//        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//
//        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneTokenStateOutputWithTheCorrectAmountAndOwner() throws Exception {
//        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);
//
//        assertEquals(nodeB.getInfo().getLegalIdentities().get(0), output.getOwner());
//        assertEquals(99, output.getAmount());
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneOutputUsingTheCorrectContract() throws Exception {
//        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//
//        assertEquals("bootcamp.TokenContract", output.getContract());
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneIssueCommand() throws Exception {
//        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getCommands().size());
//        Command command = signedTransaction.getTx().getCommands().get(0);
//
//        assert (command.getValue() instanceof TokenContract.Commands.Issue);
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneCommandWithTheIssuerAndTheOwnerAsASigners() throws Exception {
//        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getCommands().size());
//        Command command = signedTransaction.getTx().getCommands().get(0);
//
//        assertEquals(2, command.getSigners().size());
//        assertTrue(command.getSigners().contains(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey()));
//        assertTrue(command.getSigners().contains(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey()));
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() throws Exception {
//        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(0, signedTransaction.getTx().getInputs().size());
//        // The single attachment is the contract attachment.
//        assertEquals(1, signedTransaction.getTx().getAttachments().size());
//        assertNull(signedTransaction.getTx().getTimeWindow());
//    }
}