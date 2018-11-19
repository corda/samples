<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Ping-Pong CorDapp

This CorDapp allows a node to ping any other node on the network that also has this CorDapp installed.

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Pinging a node:

### RPC via Gradle:

Run the following command from the root of the project:

* Unix/Mac OSX: `./gradlew pingPartyB -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`
* Windows: `gradlew pingPartyB -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`

For example, if your node has the RPC address `localhost:10006`, you'd ping party B from a 
Unix/Mac OSX machine by running:

    `./gradlew pingPartyB -Paddress=localhost:10006 -PnodeName="O=PartyB,L=New York,C=US"`

You should see the following message, indicating that PartyB responded to your ping:

    `Successfully pinged O=PartyB,L=New York,C=US.`.

### RPC via IntelliJ:

Run the `Run Ping-Pong RPC Client` run configuration from IntelliJ. You can modify the run 
configuration to set your node's RPC address and the name of the node to ping.

You should see the following message, indicating that PartyB responded to your ping:

    `Successfully pinged O=PartyB,L=New York,C=US.`.
