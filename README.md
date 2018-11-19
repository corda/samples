This repository contains sample CorDapps created to show developers how to implement specific functionality, one per folder. These samples are all Apache 2.0 licensed, so feel free to use them as the basis for your own CorDapps.

# General

* Yo! – A simple CorDapp that allows you to send Yo’s! to other Corda nodes
* IOU – Models IOUs (I Owe yoUs) between Corda nodes (also in Java)
* Obligations – A more complex version of the IOU CorDapp (also in Java) Handles the transfer and settlement of obligations Retains participant anonymity using confidential identities (i.e. anonymous public keys)
* Negotiation – shows how multi-party negotiation is handled on the Corda ledger, in the absence of an API for user interaction

# Observers

* Crowdfunding – Use the observers feature to allow non-participants to track a crowdfunding campaign

# Attachments

* FTP – Use attachments to drag-and-drop files between Corda nodes
* Blacklist – Use an attachment to blacklist specific nodes from signing agreements

* Confidential Identities

* Whistle Blower – Use confidential identities (i.e. anonymous public keys) to whistle-blow on other nodes anonymously

# Oracles

* Prime Numbers Oracle - Use an oracle to attest to the prime-ness of integers in transaction
* Options - Use an oracle to calculate the premium on call and put options

# Scheduled Activities

* Heartbeat - Use scheduled states to cause your node to emit a heartbeat every second

# Accessing External Data

* Flow HTTP - Make an HTTP request in a flow to retrieve the Bitcoin readme from a webserver
* Access Control List - Make an HTTP request in a service to retrieve an access control list from a webserver
* Flow DB Access - Access the node’s database in flows to store and read cryptocurrency values

# Upgrading Cordapps

* Contract Upgrades - A client for upgrading contracts

# Alternate Node Web Servers

* Braid/Node.js Webserver - A node web-server using Braid and Node.js
* Spring Webserver - A node web-server using Spring that: Provides generic REST endpoints for interacting with a node via RPC Can be extended to work with specific CorDapps
* Yo Cordapp Spring Web Server - Another node web-server using Spring that: Provides REST endpoints for interacting with the Yo! CorDapp via RPC Streams vault updates to the front-end using a web-socket

# RPC Clients

* Node Info - A client for retrieving information from a running node Useful for checking that a node is running and is accessible from another host via RPC
* Ping-Pong - A client for pinging other nodes on the network to check connectivity
