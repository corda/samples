![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Access Control List Demo

A basic CorDapp to demonstrate how you can integrate access control
lists into your CorDapps.

## Background

There are three Corda nodes in this demo; PartyA, PartyB and PartyC.
There is also a web server which acts as the business network operator.
In this scenario, the business network operator does not operate a Corda
node. Instead, it serves an access control list via an API end-point.

The business network operator can load in to memory a file containing an
access control. This list will then be served via the API end-point. The
list is simply a list of serialised CordaX500 names delineated by new
lines. New names can be added via the console. Names can also be deleted
via the console. Any changes to the list will be reflected in the list
which the API end-point serves.

In this scenario, via the flow framework, the three nodes have the
capability to send each other "pings" and reply with "pongs". The three
nodes in this scenario operate a CordaService which polls the business
network operator's API end-point on a fixed interval. Each time the
access control list is downloaded, the node's cache is updated with the
new access control list. Before any of the nodes can start a flow to
sender "ping", the access control list is checked to see if the
recipient is on the list.

## How it works

The server module implements a simple API end-point which serves the
"acl.txt" file. The file is read every time a request is sent to the
`/acl` end-point. Additional lines can be added to the "acl.txt" file
while the server is running. Care must be taken to ensure that the
`CordaX500Name`s in the "acl.txt" file are well formed. If are not well
formed then the web server will return a 500 Internal Error.

The client works by polling the `/acl` end-point every second and then
updating a whitelist held within a `CordaService`. Both the `PingFlow`
and the `PongFlow` checks to see whether counter-parties are on the
white-list.

## Using the demo

Start the server and the clients. You must start the server before the
client Corda nodes:

    ./gradlew clean deployNodes
    ./gradlew runServer
    cd build/nodes
    ./runnodes

The access control list can be seen at http://localhost:8000/acl. Only
PartyA is on the whitelist initially. When all the nodes have started
then try to send a ping from PartyA to PartyB:

    start PingFlow target: PartyB

It should fail as PartyB is not on the white list:

    🚫   Done
    ☠   O=PartyB, L=New York, C=US is not on the whitelist.

Now, add PartyB to the whitelist by opening "acl.txt" in the project
root and adding `O=PartyB, L=New York, C=US` to the file on a new line.
Then, send another ping to PartyB:

    start PingFlow target: PartyB

The flow should successfully complete:

    Sending PING to O=PartyB, L=New York, C=US
    Received PONG from O=PartyB, L=New York, C=US

    ✅   Done

That's it!

