package com.example.server;

import com.example.state.IOUState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node.
 */
public class JavaClientRpc {

    private static final Logger logger = LoggerFactory.getLogger(JavaClientRpc.class);

    public static void main(String[] args) {
        //Get the node address to connect to, rpc username , rpc password via command line
        if (args.length != 3) throw new IllegalArgumentException("Usage: Client <node address> <rpc username> <rpc password>");

        NetworkHostAndPort networkHostAndPort = NetworkHostAndPort.parse(args[0]);
        String rpcUsername = args[1];
        String rpcPassword = args[2];

        /*get the client handle which has the start method
        Secure SSL connection can be established with the server if specified by the client.
        This can be configured by specifying the truststore path containing the RPC SSL certificate in the while creating CordaRPCClient instance.*/
        CordaRPCClient client = new CordaRPCClient(networkHostAndPort);

        //start method establishes conenction with the server, starts off a proxy handler and return a wrapper around proxy.
        CordaRPCConnection rpcConnection = client.start(rpcUsername, rpcPassword);

        //proxy is used to convert the client high level calls to artemis specific low level messages
        CordaRPCOps proxy = rpcConnection.getProxy();

        //hit the node to retrieve network map
        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        logger.info("All the nodes available in this network", nodes);

        //hit the node to get snapshot and observable for IOUState
        DataFeed<Vault.Page<IOUState>, Vault.Update<IOUState>> dataFeed = proxy.vaultTrack(IOUState.class);

        //this gives a snapshot of IOUState as of now. so if there are 11 IOUState as of now, this will return 11 IOUState objects
        Vault.Page<IOUState> snapshot = dataFeed.getSnapshot();

        //this returns an observable on IOUState
        Observable<Vault.Update<IOUState>> updates = dataFeed.getUpdates();

        // call a method for each IOUState
        snapshot.getStates().forEach(JavaClientRpc::actionToPerform);

        //perform certain action for each update to IOUState
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(JavaClientRpc::actionToPerform));
    }

    /**
     * Perform certain action because of any update to Observable IOUState
     * @param state
     */
    private static void actionToPerform(StateAndRef<IOUState> state) {
        logger.info("{}", state.getState().getData());
    }
}
