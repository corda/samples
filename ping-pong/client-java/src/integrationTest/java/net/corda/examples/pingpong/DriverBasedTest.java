package net.corda.examples.pingpong;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.driver.Driver.driver;

public class DriverBasedTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void runDriverTest() {
        thrown.expect(IllegalArgumentException.class);

        TestIdentity bankA = new TestIdentity(new CordaX500Name("Bank A", "", "GB"));
        TestIdentity bankB = new TestIdentity(new CordaX500Name("Bank B", "", "GB"));
        TestIdentity bankC = new TestIdentity(new CordaX500Name("Bank C", "", "GB"));

        User user = new User("user1", "test", Collections.singleton("ALL"));

        driver(new DriverParameters().withStartNodesInProcess(true), dsl -> {

            NodeHandle a = null;

            try {
                a = dsl.startNode(new NodeParameters().withProvidedName(bankA.getName()).withRpcUsers(Collections.singletonList(user))).get();
                dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()).withRpcUsers(Collections.singletonList(user))).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            String nodeARpcAddress = a.getRpcAddress().toString();
            Client.RPCClient nodeARpcClient = new Client.RPCClient(nodeARpcAddress);

            // We can ping Bank B...
            nodeARpcClient.ping(bankB.getName().toString());
            // ...but not Bank C, who isn't on the network
            nodeARpcClient.ping(bankC.getName().toString());
            return null;
        });
    }
}
