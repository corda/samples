<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Pigtail

A simple Braid service and JS client example.

[Braid](https://gitlab.com/bluebank/braid) allows you to easily define a 
server running on your node that serves up flow and other endpoints that can 
be consumed in either Java or JavaScript. 

We define our 
[Braid server](https://github.com/joeldudleyr3/pigtail/blob/master/cordapp/src/main/kotlin/com/template/BootstrapBraidService.kt) 
as follows:

    @CordaService
    class BootstrapBraidService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
        init {
            BraidConfig()
                    // Include a flow on the Braid server.
                    .withFlow(WhoAmIFlow::class.java)
                    // Include a service on the Braid server.
                    .withService("myService", BraidService(serviceHub))
                    // The port the Braid server listens on.
                    .withPort(8080)
                    // Using http instead of https.
                    .withHttpServerOptions(HttpServerOptions().setSsl(false))
                    // Start the Braid server.
                    .bootstrapBraid(serviceHub)
        }
    }
    
After starting the node as normal, we consume its endpoints using a 
[Node.js client](https://github.com/joeldudleyr3/pigtail/blob/master/client-js/client.js).

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the node:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Running the client:

* Install `npm` and `Node.js` by following the instructions here: 
  https://www.npmjs.com/get-npm
* Open a command-line terminal window
* Move into the `client-js` folder
* Install the dependencies using `npm install`
* Run the client using `node client.js`

## Interacting with the node:

The client exposes three endpoints:

* `localhost:3000/whoami-flow-callback`, which uses Braid to call the `WhoAmI`
  flow on the node, and handles the response using callbacks
* `localhost:3000/whoami-flow-promise`, which uses Braid to call the `WhoAmI`
  flow on the node, and handles the response using promises
* `localhost:3000/whoami-service`, which uses Braid to call the `BraidService` 
  on the node, and handles the response using callbacks
