package net.corda.yo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jdk.nashorn.internal.ir.annotations.Immutable;
import net.corda.core.identity.CordaX500Name;
import net.corda.node.internal.Node;
import net.corda.testing.driver.Driver;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.node.User;

import static net.corda.testing.driver.Driver.driver;
import static net.corda.testing.node.internal.DriverDSLImplKt.startNode;

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Run the "Run Template CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports for each node, which should be output to the console. The "Debug CorDapp" configuration runs
 *    with port 5007, which should be "NodeA". In any case, double-check the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
/*
public class YoApp {
    public static void main(String[] args){
        User user = new User("user1","test", ImmutableSet.of("ALL"));
        DriverParameters driverParameters = new DriverParameters();
        driverParameters.withIsDebug(true);
        driverParameters.withWaitForAllNodesToFinish(true);
        Node nodeA = startNode();

        driver(driverParameters,{
            startNode(new CordaX500Name("PartyA", "London", "GB"), ImmutableList.of(user)));
            startNode(new CordaX500Name("PartyB", "New York", "US"), ImmutableList.of(user)))
        });
    }
}

/*
fun main(args: Array<String>) {
        // No permissions required as we are not invoking flows.
        val user = User("user1", "test", permissions = setOf("ALL"))
        driver(DriverParameters(isDebug = true, waitForAllNodesToFinish = true)) {
        val (nodeA, nodeB) = listOf(
        startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = listOf(user)),
        startNode(providedName = CordaX500Name("PartyB", "New York", "US"), rpcUsers = listOf(user))).map { it.getOrThrow() }
        }
        }
*/