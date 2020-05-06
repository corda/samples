package net.corda.examples.obligation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;

public class IntegrationTest {
    private final User user = new User("user1", "test", ImmutableSet.of("ALL"));

    @Test
    public void runDriverTest() {
        final CordaX500Name nodeAName = new CordaX500Name("NodeA", "", "GB");
        final CordaX500Name nodeBName = new CordaX500Name("NodeB", "", "US");

        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            List<CordaFuture<NodeHandle>> handles = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(nodeAName).withRpcUsers(ImmutableList.of(user))),
                    dsl.startNode(new NodeParameters().withProvidedName(nodeBName).withRpcUsers(ImmutableList.of(user)))
            );

            try {
                NodeHandle nodeAHandle = handles.get(0).get();
                NodeHandle nodeBHandle = handles.get(1).get();

                // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
                // started and can communicate. This is a very basic test, in practice tests would be starting flows,
                // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
                // as intended.
                Assert.assertEquals(nodeAHandle.getRpc().wellKnownPartyFromX500Name(nodeBName).getName(), nodeBName);
                Assert.assertEquals(nodeBHandle.getRpc().wellKnownPartyFromX500Name(nodeAName).getName(), nodeAName);
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }
}