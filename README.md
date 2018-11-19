This repository contains sample CorDapps created to show developers how to implement specific functionality, one per folder. These samples are all Apache 2.0 licensed, so feel free to use them as the basis for your own CorDapps.

# General

* `yo-cordapp`: Yo! – A simple CorDapp that allows you to send Yo’s! to other Corda nodes
* `cordapp-example`: IOU – Models IOUs (I Owe yoUs) between Corda nodes (also in Java)
* `obligation-cordapp`: Obligations – A more complex version of the IOU CorDapp (also in Java) Handles the transfer and settlement of obligations Retains participant anonymity using confidential identities (i.e. anonymous public keys)
* `negotiation-cordapp`: Negotiation – shows how multi-party negotiation is handled on the Corda ledger, in the absence of an API for user interaction

# Observers

* `observable-states`: Crowdfunding – Use the observers feature to allow non-participants to track a crowdfunding campaign

# Attachments

* `cordaftp`: FTP – Use attachments to drag-and-drop files between Corda nodes
* `blacklist`: Blacklist – Use an attachment to blacklist specific nodes from signing agreements

* Confidential Identities

* `whistleblower`: Whistle Blower – Use confidential identities (i.e. anonymous public keys) to whistle-blow on other nodes anonymously

# Oracles

* `oracle-example`: Prime Numbers Oracle - Use an oracle to attest to the prime-ness of integers in transaction
* `cordapp-option`: Options - Use an oracle to calculate the premium on call and put options

# Scheduled Activities

* `heartbeat`: Heartbeat - Use scheduled states to cause your node to emit a heartbeat every second

# Accessing External Data

* `flow-http`: Flow HTTP - Make an HTTP request in a flow to retrieve the Bitcoin readme from a webserver
* `acl-demo`: Access Control List - Make an HTTP request in a service to retrieve an access control list from a webserver
* `flow-db`: Flow DB Access - Access the node’s database in flows to store and read cryptocurrency values

# Upgrading Cordapps

* `contract-upgrades`: Contract Upgrades - A client for upgrading contracts

# Alternate Node Web Servers

* `pigtail`: Braid/Node.js Webserver - A node web-server using Braid and Node.js
* `spring-webserver`: Spring Webserver - A node web-server using Spring that: Provides generic REST endpoints for interacting with a node via RPC Can be extended to work with specific CorDapps
* `spring-observable-stream`: Yo Cordapp Spring Web Server - Another node web-server using Spring that: Provides REST endpoints for interacting with the Yo! CorDapp via RPC Streams vault updates to the front-end using a web-socket

# RPC Clients

* `corda-nodeinfo`: Node Info - A client for retrieving information from a running node Useful for checking that a node is running and is accessible from another host via RPC
* `ping-pong`: Ping-Pong - A client for pinging other nodes on the network to check connectivity
