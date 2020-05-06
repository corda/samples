<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Ping-Pong CorDapp

This CorDapp allows a node to ping any other node on the network that also has this CorDapp installed.

It demonstrates how to use Corda for messaging and passing data using a flow without saving any states or using any contracts.

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

Java use the `workflows-java:deployNodes` task and `./workflows-java/build/nodes/runnodes` script.

Kotlin use the `workflows-kotlin:deployNodes` task and `./workflows-kotlin/build/nodes/runnodes` script.

## Pinging a node:

### Pinging from the shell
Run the following command from PartyA's shell:
```
start ping counterparty: PartyB
```
Since we are not using any start or transaction, if we want to see the trace of the action. We can look it up from `/build/nodes/PartyA/logs/XXXXX.log` it will be something like: 
```
[pool-8-thread-1] shell.FlowShellCommand. - Executing command "flow start ping counterparty: PartyB",
```


### RPC via Gradle:

Run the following command from the root of the project:

* Unix/Mac OSX: `./gradlew pingPartyB<Java|Kotlin> -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`
* Windows: `gradlew pingPartyB<Java|Kotlin> -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`

For example, if your node has the RPC address `localhost:10006`, you'd ping party B from a 
Unix/Mac OSX machine by running:

    ./gradlew pingPartyBKotlin -Paddress=localhost:10006 -PnodeName="O=PartyB,L=New York,C=US"

You should see the following message, indicating that PartyB responded to your ping:

    Successfully pinged O=PartyB,L=New York,C=US..

### RPC via IntelliJ:

Run the `Run Ping-Pong RPC Client` run configuration from IntelliJ. You can modify the run 
configuration to set your node's RPC address and the name of the node to ping.

You should see the following message, indicating that PartyB responded to your ping:

    `Successfully pinged O=PartyB,L=New York,C=US.`.
