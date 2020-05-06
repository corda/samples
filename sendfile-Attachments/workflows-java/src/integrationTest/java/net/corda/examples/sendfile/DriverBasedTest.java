package net.corda.examples.sendfile;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.Driver;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DriverBasedTest {
    private final TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "", "GB"));
    private final TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "", "US"));

    @Test
    public void nodeTest() {
        Driver.driver(new DriverParameters().withStartNodesInProcess(true), driver -> {
            NodeHandle partyAHandle;
            NodeHandle partyBHandle;

            try {
                // Start a pair of nodes and wait for them both to be ready.
                partyAHandle = driver.startNode(new NodeParameters().withProvidedName(bankA.getName())).get();
                partyBHandle = driver.startNode(new NodeParameters().withProvidedName(bankB.getName())).get();
            } catch (Exception e) {
                throw new IllegalArgumentException();
            }

            // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
            // nodes have started and can communicate.

            // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
            // and other important metrics to ensure that your CorDapp is working as intended.
            assertEquals(bankB.getName(), partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()).getName());
            assertEquals(bankA.getName(), partyBHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()).getName());
            return null;
        });


    }
}
