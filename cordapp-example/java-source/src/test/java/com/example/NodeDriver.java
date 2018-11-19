package com.example;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;

/**
 * This file is exclusively for being able to run your nodes through an IDE.
 * Do not use in a production environment.
 */
public class NodeDriver {
    public static void main(String[] args) {
        final User user = new User("user1", "test", ImmutableSet.of("ALL"));
        driver(new DriverParameters().withWaitForAllNodesToFinish(true), dsl -> {
                    List<CordaFuture<NodeHandle>> nodeFutures = ImmutableList.of(
                            dsl.startNode(new NodeParameters()
                                    .withProvidedName(new CordaX500Name("PartyA", "London", "GB"))
                                    .withCustomOverrides(ImmutableMap.of("rpcSettings.address", "localhost:10008", "rpcSettings.adminAddress", "localhost:10048", "webAddress", "localhost:10009"))
                                    .withRpcUsers(ImmutableList.of(user))),
                            dsl.startNode(new NodeParameters()
                                    .withProvidedName(new CordaX500Name("PartyB", "New York", "US"))
                                    .withCustomOverrides(ImmutableMap.of("rpcSettings.address", "localhost:10011", "rpcSettings.adminAddress", "localhost:10051", "webAddress", "localhost:10012"))
                                    .withRpcUsers(ImmutableList.of(user))),
                            dsl.startNode(new NodeParameters()
                                    .withProvidedName(new CordaX500Name("PartyC", "Paris", "FR"))
                                    .withCustomOverrides(ImmutableMap.of("rpcSettings.address", "localhost:10014", "rpcSettings.adminAddress", "localhost:10054", "webAddress", "localhost:10015"))
                                    .withRpcUsers(ImmutableList.of(user))));

                    try {
                        dsl.startWebserver(nodeFutures.get(0).get());
                        dsl.startWebserver(nodeFutures.get(1).get());
                        dsl.startWebserver(nodeFutures.get(2).get());

                    } catch (Throwable e) {
                        System.err.println("Encountered exception in node startup: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return null;
                }
        );
    }
}
