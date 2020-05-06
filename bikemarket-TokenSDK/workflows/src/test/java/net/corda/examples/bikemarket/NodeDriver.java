package net.corda.examples.bikemarket;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import static net.corda.testing.driver.Driver.driver;

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
public class NodeDriver {
    public static void main(String[] args) {
        final List<User> rpcUsers =
                ImmutableList.of(new User("user1", "test", ImmutableSet.of("ALL")));

        driver(new DriverParameters().withStartNodesInProcess(true).withWaitForAllNodesToFinish(true), dsl -> {
                    try {
                        dsl.startNode(new NodeParameters()
                                .withProvidedName(new CordaX500Name("PartyA", "London", "GB"))
                                .withRpcUsers(rpcUsers)).get();
                        dsl.startNode(new NodeParameters()
                                .withProvidedName(new CordaX500Name("PartyB", "New York", "US"))
                                .withRpcUsers(rpcUsers)).get();
                    } catch (Throwable e) {
                        System.err.println("Encountered exception in node startup: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return null;
                }
        );
    }
}
