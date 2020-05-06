package net.corda.examples.sendfile;

import com.google.common.collect.ImmutableSet;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.Driver;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
public class NodeDriver {
    public static void main(String[] args) {
        List<User> rpcUsers = Collections.singletonList((new User("user1", "test", ImmutableSet.of("ALL"))));

        Driver.driver(new DriverParameters().withStartNodesInProcess(true).withWaitForAllNodesToFinish(true), driver -> {

            try {
                driver.startNode(new NodeParameters().withProvidedName(new CordaX500Name("PartyA", "London", "GB")).withRpcUsers(rpcUsers)).get();
                driver.startNode(new NodeParameters().withProvidedName(new CordaX500Name("PartyB", "New York", "US")).withRpcUsers(rpcUsers)).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            return null;
        });
    }
}
