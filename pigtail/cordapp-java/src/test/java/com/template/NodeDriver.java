package com.template;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;

import java.util.Collections;

import static net.corda.testing.driver.Driver.driver;

public class NodeDriver {
    public static void main(String[] args) {
        CordaX500Name nodeName = new CordaX500Name("PartyA", "London", "GB");
        User rpcUser = new User("user1", "test", ImmutableSet.of("ALL"));
        DriverParameters driverParameters = new DriverParameters()
                .withWaitForAllNodesToFinish(true)
                .withNotarySpecs(Collections.emptyList());

        driver(driverParameters, dsl -> {
            CordaFuture<NodeHandle> future = dsl.startNode(new NodeParameters().withProvidedName(nodeName).withRpcUsers(ImmutableList.of(rpcUser)));
            try {
                future.get();
            } catch (Throwable e) {
                System.err.println("Encountered exception in node startup: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });

    }
}
