<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Blacklist CorDapp

This CorDapp allows nodes to reach agreement over arbitrary strings of text, but only with parties that are not 
included in the blacklist uploaded to the nodes.

The blacklist takes the form of a jar including a single file, `blacklist.txt`. `blacklist.txt` lists the following 
parties as being banned from entering into agreements:

* Crossland Savings
* TCF National Bank Wisconsin
* George State Bank
* The James Polk Stone Community Bank
* Tifton Banking Company

The blacklist jar is uploaded as an attachment when building a transaction, and used in the `AgreementContract` to 
check that the parties to the `AgreementState` are not blacklisted.

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Uploading the blacklist:

Before attempting to reach any agreements, you must upload the blacklist as an attachment to each node that you want to 
be able to *initiate* an agreement. The blacklist can be uploaded via RPC or HTTP.

### Via RPC

Run the following command from the project's root folder:

* Unix/Mac OSX: `./gradlew uploadBlacklist`
* Windows: `gradlew uploadBlacklist`

You should see three messages of the form `Blacklist uploaded to node via localhost:100XX`.

### Via HTTP

Each node exposes a front-end for uploading the blacklist:

* Monogram Bank: `localhost:10008/web/a`
* Hiseville Deposit Bank: `localhost:10011/web/a`
* George State Bank: `localhost:10014/web/a`

For the node you want to upload the blacklist to:

* Click `Choose file` to select the .jar containing the blacklist (under `src/main/resources/blacklist.jar`)
* Click `Upload blacklist` to upload it to the node

## Interacting with the nodes:

You can now interact with this CorDapp using its web API. Each node exposes this web API on a different address:

* Monogram Bank: `localhost:10008/`
* Hiseville Deposit Bank: `localhost:10011/`
* George State Bank: `localhost:10014/`

Note that George State Bank is a blacklisted entity, and the `AgreementContract` will prevent it from entering into 
agreements with other nodes.

The web API for each node exposes two endpoints:

* `/api/a/propose-agreement?counterparty=X&agreement=Y`, which causes the node to reach agreement Y with counterparty X
* `/api/a/agreements`, which lists the node's existing `AgreementState`s

For example, Monogram Bank and Hiseville Deposit Bank may enter into an agreement by visiting the following URL:

    http://localhost:10008/api/a/propose-agreement?counterparty=Hiseville Deposit Bank&agreement=A and B agree Y

You should see the following message:

    Agreement reached.

If you now visit `http://localhost:10008/api/a/agreements`, you should see the agreement stored on the node:

    [ {
      "partyA" : "C=GB,L=London,O=Monogram Bank",
      "partyB" : "C=BR,L=Sao Paulo,O=Hiseville Deposit Bank",
      "txt" : "A and B agree Y",
      "participants" : [ "C=GB,L=London,O=Monogram Bank", "C=BR,L=Sao Paulo,O=Hiseville Deposit Bank" ]
    } ]
    
However, if you visit the following URL to attempt to enter into an agreement with George State Bank:

    http://localhost:10008/api/a/propose-agreement?counterparty=George State Bank&agreement=A and B agree Y
    
You will see the following message:

    Contract verification failed: Failed requirement: The agreement involved blacklisted parties: [George State Bank]

And no agreement will be stored!

# To-Do

* Currently, the blacklist jar's hash is hardcoded in the contract. An improvement would be to support any jar signed 
  by a specific trusted node
  
