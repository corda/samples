package net.corda.examples.sendfile;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.sendfile.flows.DownloadAttachment;
import net.corda.examples.sendfile.flows.SendAttachment;
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
            TestCordapp.findCordapp("net.corda.examples.sendfile.contracts"),
            TestCordapp.findCordapp("net.corda.examples.sendfile.flows")
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

    //Test #1 check attachments list has more than one element
    //one for contract attachment, another one for attached zip
    //@TODO Make sure change the file path in the SendAttachment flow to "../test.zip" for passing the unit test.
    //because the unit test are in a different working directory than the running node.
    @Test
    public void attachmentListHasMoreThanOneElement() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new SendAttachment(b.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        SignedTransaction ptx = future.get();
        assert (ptx.getTx().getAttachments().size() > 1);
    }

    //Test #2 test successful download of the attachment by the receiving node.
    @Test
    public void attachmentDownloadedByBuyer() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new SendAttachment(b.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        CordaFuture<String> future1 = b.startFlow(new DownloadAttachment(a.getInfo().getLegalIdentities().get(0), "file.zip"));
        network.runNetwork();
        String result = future1.get();
        System.out.println(result);
    }
}
