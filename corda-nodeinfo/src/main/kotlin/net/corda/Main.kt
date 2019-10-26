package net.corda

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.apache.activemq.artemis.api.core.client.ActiveMQClient
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory
import org.apache.activemq.artemis.api.core.TransportConfiguration
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants

/**
 * Confirms you can connect to a node, prints out some basic information
 * Usage: <Program> host:port user password
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        throw RuntimeException("Usage: <Program> host:port username password (or via ./gradlew getInfo host:port username password)")
    }

    val host = args[0]
    val username = args[1]

    val password =
            if (args.size > 2) {
                args[2]
            } else {
                print("Password:")
                System.console().readPassword().joinToString(separator = "")
            }

    println("Logging into $host as $username")

    val proxy = loginToCordaNode(host, username, password)

    println("Node connected: ${proxy.nodeInfo().legalIdentities.first()}")

    println("Time: ${proxy.currentNodeTime()}.")

    println("Flows: ${proxy.registeredFlows()}")

    println("Platform version: ${proxy.nodeInfo().platformVersion}")

    println("Current Network Map Status -->")
    proxy.networkMapSnapshot().map {
        println("-- ${it.legalIdentities.first().name} @ ${it.addresses.first().host}")
    }

    println("Registered Notaries -->")
    proxy.notaryIdentities().map {
        println("-- ${it.name}")
    }

    if (args.getOrNull(3) == "extended") {
        println("Platform version: ${proxy.nodeInfo().platformVersion}")
        println(proxy.currentNodeTime())
    }
}

fun loginToCordaNode(host: String, username: String, password: String): CordaRPCOps {
    val nodeAddress = NetworkHostAndPort.parse(host)
    val client = CordaRPCClient(nodeAddress)
    return client.start(username, password).proxy
}

/**
 * Try and connect directly to the queues
 */
fun amqp(host: String) {
    val connectionParams = HashMap<String, Any>()
    val port = host.split(":")[1]
    val hostname = host.split(":")[0]
    println("$hostname, $port")
    connectionParams[TransportConstants.PORT_PROP_NAME] = port
    connectionParams[TransportConstants.HOST_PROP_NAME] = hostname
    val tc = TransportConfiguration(NettyConnectorFactory::class.java.name, connectionParams)
    val locator = ActiveMQClient.createServerLocatorWithoutHA(tc)
    val queueFactory = locator.createSessionFactory()

    println("Factory closed = ${queueFactory.isClosed}")
    println("Connected ok.")
}

