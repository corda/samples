package net.corda.examples.pingpong;

import kotlin.Unit;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.examples.pingpong.flows.Ping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class Client {
    private static final String RPC_USERNAME = "user1";
    private static final String RPC_PASSWORD = "test";

    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Usage: RpcClient <node address> <counterpartyName>");
        System.out.println(args[0]);
        System.out.println(args[1]);

        final String rpcAddressString = args[0];
        final String counterpartyName = args[1];

        final RPCClient rpcClient = new RPCClient(rpcAddressString);
        rpcClient.ping(counterpartyName);
        rpcClient.closeRpcConnection();
    }

    /** An example RPC client that connects to a node and performs various example operations. */
    static class RPCClient {
        public static Logger logger = LoggerFactory.getLogger(RPCClient.class);

        private CordaRPCConnection rpcConnection;

        /** Sets a [CordaRPCConnection] to the node listening on [rpcPortString]. */
        protected RPCClient(String rpcAddressString) {
            final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(rpcAddressString);
            final CordaRPCClient client = new CordaRPCClient(nodeAddress);
            rpcConnection = client.start(RPC_USERNAME, RPC_PASSWORD);
        }

        protected void closeRpcConnection() {
            rpcConnection.close();
        }

        protected void ping(String counterpartyName) {
            CordaRPCOps rpcProxy = rpcConnection.getProxy();
            pingCounterparty(rpcProxy, counterpartyName);

            System.out.println("\nSuccessfully pinged " + counterpartyName);
            logger.info("\nSuccessfully pinged " + counterpartyName);
        }

        private void pingCounterparty(CordaRPCOps rpcProxy, String coutnerpartyName) {
            final CordaX500Name counterpartyX500Name = CordaX500Name.parse(coutnerpartyName);
            Party counterparty = rpcProxy.wellKnownPartyFromX500Name(counterpartyX500Name);
            if (counterparty == null) throw new IllegalArgumentException("Peer " + coutnerpartyName + " not found in the network map");

            CordaFuture<Void> flowFuture = rpcProxy.startFlowDynamic(Ping.class, counterparty).getReturnValue();
            try {
                flowFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

    }
}
