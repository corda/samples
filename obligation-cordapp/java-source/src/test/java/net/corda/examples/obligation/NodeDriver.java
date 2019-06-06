package net.corda.examples.obligation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.User;

import static net.corda.testing.driver.Driver.driver;

public class NodeDriver {
    public static void main(String[] args) {
        final User user = new User("user1", "test", ImmutableSet.of("ALL"));

        driver(new DriverParameters()
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.finance.contracts.asset"),
                        TestCordapp.findCordapp("net.corda.finance.schemas")))
                .withIsDebug(true).withStartNodesInProcess(true)
                .withWaitForAllNodesToFinish(true), dsl -> {
            try {
                NodeHandle nodeA = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyA", "London", "GB"))
                        .withRpcUsers(ImmutableList.of(user))).get();
                NodeHandle nodeB = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyB", "New York", "US"))
                        .withRpcUsers(ImmutableList.of(user))).get();
                NodeHandle nodeC = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyC", "Paris", "FR"))
                        .withRpcUsers(ImmutableList.of(user))).get();

                dsl.startWebserver(nodeA);
                dsl.startWebserver(nodeB);
                dsl.startWebserver(nodeC);
            } catch (Throwable e) {
                System.err.println("Encountered exception in node startup: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }
}