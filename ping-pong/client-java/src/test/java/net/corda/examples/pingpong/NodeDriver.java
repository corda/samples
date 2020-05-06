package net.corda.examples.pingpong;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.driver.Driver.driver;

public class NodeDriver {
    public static void main(String[] args) {
        final List<User> rpcUsers = Arrays.asList(new User("user1", "test", Collections.singleton("ALL")));

        driver(new DriverParameters().withStartNodesInProcess(true).withWaitForAllNodesToFinish(true), dsl -> {

            try {
                dsl.startNode(new NodeParameters().withProvidedName(new CordaX500Name("PartyA", "London", "GB"))
                        .withRpcUsers(rpcUsers)).get();
                dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyB", "New York", "US"))
                        .withRpcUsers(rpcUsers)).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            return null;
        });


    }
}
