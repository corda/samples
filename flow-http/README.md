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

Java use the `workflows-java:deployNodes` task and `./workflows-java/build/nodes/runnodes` script.

Kotlin use the `workflows-kotlin:deployNodes` task and `./workflows-kotlin/build/nodes/runnodes` script.

## Interacting with the nodes:

We'll be interacting with the node via its interactive shell.

To have the node use a flow to retrieve the HTTP of the original Bitcoin URL, run the following command in the node's 
shell:

    start HttpCallFlow

The text of the first commit of the BitCoin readme will be printed to the terminal window.
