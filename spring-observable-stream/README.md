<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Spring webserver

This project defines a simple Spring webserver that connects to a Corda node via RPC.

# Structure:

The web server is set up to interact with the Yo! CorDapp (see https://www.corda.net/samples/), 
which is included in the project in the `yo` module.

The Spring web server is defined in the `server` module, and has two parts:

* `src/main/resources/static`, which defines the webserver's frontend
* `src/main/kotlin/net/corda/server`, which defines the webserver's backend

The backend has two controllers, defined in `server/src/main/kotlin/net/corda/server/Controller.kt`:

* `RestController`, which manages standard REST requests. It defines four endpoints:
    * GET `yo/me/`, to retrieve the node's identity
    * GET `yo/peers/`, to retrieve the node's network peers
    * GET `yo/getyos/`, to retrieve any Yo's from the node's vault
    * POST `yo/sendyo/`, to send a Yo to another node
    
* `StompController`, which defines a a single endpoint, `/stomp/streamyos`. Our web frontend hits 
  this endpoint automatically when it loads. This causes the webserver to retrieve an observable 
  of the node's vault via RPC and subscribe to it for updates. Whenever the observable emits a 
  notification of a new Yo, the update is streamed to the frontend over a web-socket
  
# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Running the webservers:

Once the nodes are running, you can start the node webservers from the command line:

* Windows: `gradlew.bat runPartyAServer` and `gradlew.bat runPartyBServer`
* Unix: `./gradlew runPartyAServer` and `./gradlew runPartyBServer`

You can also start the webservers using the `Run PartyA Server` and `Run PartyB Server` IntelliJ 
run configurations.

Both approaches use environment variables to set:

* `server.port`, which defines the HTTP port the webserver listens on
* `config.rpc.port`, which defines the RPC port the webserver uses to connect to the node

## Interacting with the nodes:

Once the nodes are started, you can access the node's frontends at the following addresses:

* PartyA: `localhost:8080`
* PartyB: `localhost:8081`

Sending a Yo to a counterparty will initiate the following sequence of events:

* The counterparty node will store the new Yo in their vault
* The observable on the node's vault will emit a notification
* The webserver, which has subscribed to the observable, receives the notification
* The webserver streams the update to the front-end over the websocket
* The frontend updates itself automatically to display the new Yo
