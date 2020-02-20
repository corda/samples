package net.corda.examples.whistleblower;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import org.junit.Test;
import net.corda.testing.driver.Driver;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class DriverBasedTest {
    private final TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "", "GB"));
    private final TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "", "US"));

    @Test
    public void nodeTest() {
        // Start a pair of nodes and wait for them both to be ready.
        Driver.driver(new DriverParameters().withStartNodesInProcess(true).withIsDebug(true), driver -> {
            NodeHandle partyAHandle = null;
            NodeHandle partyBHandle = null;
            try {
                partyAHandle = driver.startNode(new NodeParameters().withProvidedName(bankA.getName())).get();
                partyBHandle = driver.startNode(new NodeParameters().withProvidedName(bankB.getName())).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
            // nodes have started and can communicate.

            // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
            // and other important metrics to ensure that your CorDapp is working as intended.
            assert partyAHandle != null;
            assert partyBHandle != null;
            assertEquals(bankB.getName(), Objects.requireNonNull(partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName())).getName());
            assertEquals(bankA.getName(), Objects.requireNonNull(partyBHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName())).getName());
            return null;
        });
    }
}
