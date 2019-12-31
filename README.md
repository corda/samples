This repository contains sample CorDapps created to show developers how to implement specific functionality, one per folder. These samples are all Apache 2.0 licensed, so feel free to use them as the basis for your own CorDapps.

# General

* `yo-cordapp`: A simple CorDapp that allows you to send Yo’s! to other Corda nodes
* `cordapp-example`: Models IOUs (I Owe yoUs) between Corda nodes (also in Java)
* `obligation-cordapp`: A more complex version of the IOU CorDapp (also in Java) Handles the transfer and settlement of obligations Retains participant anonymity using confidential identities (i.e. anonymous public keys)
* `negotiation-cordapp`: Shows how multi-party negotiation is handled on the Corda ledger, in the absence of an API for user interaction
* `ping-pong`: Demonstrates the messaging functionality within the flow framework.

# Observers

* `observable-states`: Use the observers feature to allow regulators to track regulated activity

# Attachments

* `blacklist`: Use an attachment to blacklist specific nodes from signing agreements

# Confidential identities

* `whistleblower`: Use confidential identities (i.e. anonymous public keys) to whistle-blow on other nodes anonymously

# Oracles

* `oracle-example`: Use an oracle to attest to the prime-ness of integers in transaction

# Scheduled activities

* `heartbeat`: Use scheduled states to cause your node to emit a heartbeat every second

# Queryable State
* `queryable-states`: Persisting ContractState information to custom database table using QueryableState.

# Accessing external data

* `flow-http`: Make an HTTP request in a flow to retrieve the Bitcoin readme from a webserver
* `flow-db`: Access the node’s database in flows to store and read cryptocurrency values

# Upgrading Cordapps

* `explicit-cordapp-upgrades`: A client for upgrading contracts using the Contract Upgrade Flow
* `implicit-cordapp-upgrades`: An app with a number of different versions, showing how to carry out various upgrade procedures

# Interacting with your node

## Web servers

* `pigtail`: A node web-server using Braid and Node.js
* `spring-webserver`: A node web-server using Spring that provides generic REST endpoints for interacting with a node via RPC and can be extended to work with specific CorDapps

## Command-line clients

* `corda-nodeinfo`: A command-line client for retrieving information from a running node Useful for checking that a node is running and is accessible from another host via RPC

## Reference States

* `reference-states`: A cordapp demonstrating the use of Reference States to extend the IOU `cordapp-example` cordapp
