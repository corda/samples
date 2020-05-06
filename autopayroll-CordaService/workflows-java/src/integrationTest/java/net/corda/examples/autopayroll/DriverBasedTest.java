package net.corda.examples.autopayroll;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import org.junit.Test;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

import java.util.List;

public class DriverBasedTest {
    private TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "", "GB"));
    private TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "", "US"));


    @Test
    public void nodeTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {

            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
                // started and can communicate. This is a very basic test, in practice tests would be starting flows,
                // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
                // as intended.
                assertEquals(partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()).getName(), bankB.getName());
                assertEquals(partyBHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()).getName(), bankA.getName());
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }
}
