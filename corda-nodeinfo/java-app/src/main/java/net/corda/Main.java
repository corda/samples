package net.corda;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        if (args.length <= 2) {
            throw new RuntimeException("Usage: <Program> host:port username password (or via ./gradlew getInfo host:port username password)");
        }

        final String host = args[0];
        final String username = args[1];
        final String password;

        if (args.length > 2) password = args[2];
        else {
            System.out.print("Password:");
            password = System.console().readPassword().toString();
        }

        System.out.println("Logging into " + host + " as " + username);

        CordaRPCOps proxy = loginToCordaNode(host, username, password);

        System.out.println("Node connected: " + proxy.nodeInfo().getLegalIdentities().get(0));

        System.out.println("Time: " + proxy.currentNodeTime());

        System.out.println("Flows: " + proxy.registeredFlows());

        System.out.println("Platform version: " + proxy.nodeInfo().getPlatformVersion());

        System.out.println("Current Network Map Status --> ");
        proxy.networkMapSnapshot().iterator().forEachRemaining(it -> {
            System.out.println("-- " + it.getLegalIdentities().get(0) + " @ " + it.getAddresses().get(0).getHost());
        });

        System.out.println("Registered Notaries --> ");
        proxy.notaryIdentities().iterator().forEachRemaining(it -> {
            System.out.println("-- " + it.getName());
        });

        if (args.length >= 4) {
            if (args[3].equals("extended")) {
                System.out.println("Platform version: " + proxy.nodeInfo().getPlatformVersion());
                System.out.println(proxy.currentNodeTime());
            }
        }

    }

    private static CordaRPCOps loginToCordaNode(String host, String username, String password) {
        NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(host);
        CordaRPCClient client = new CordaRPCClient(nodeAddress);
        return client.start(username, password).getProxy();
    }

    /**
     * Try and connect directly to the queues
     */
    static void amqp(String host) {
        HashMap<String, Object> connectionParams = new HashMap<>();
        String port = host.split(":")[1];
        String hostname = host.split(":")[0];
        System.out.println(hostname + ", " + port);
        connectionParams.put(TransportConstants.PORT_PROP_NAME, port);
        connectionParams.put(TransportConstants.HOST_PROP_NAME, hostname);
        TransportConfiguration tc = new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams);
        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(tc);
        try {
            ClientSessionFactory queueFactory = locator.createSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
