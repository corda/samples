<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Flow Http CorDapp

This CorDapp provides a simple example of how HTTP requests can be made in flows. In this case, the flow makes an HTTP 
request to retrieve the original BitCoin readme from GitHub.

Be aware that support of HTTP requests in flows is currently limited:

* The request must be executed in a BLOCKING way. Flows don't currently support suspending to await an HTTP call's 
  response
* The request must be idempotent. If the flow fails and has to restart from a checkpoint, the request will also be 
  replayed

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes:

### Via RPC

Run the following command from a terminal window at the root of the project:

* Unix/Mac OSX: `./gradlew runClient`
* Windows: `gradlew runClient`

The text of the first commit of the BitCoin readme will be printed to the terminal window.

### Via IntelliJ

Run the `Run Flow Http RPC Client` run configuration. As with the RPC client, the text of the first commit of the 
BitCoin readme will be printed in the terminal window.
