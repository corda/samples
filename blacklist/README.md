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
be able to *initiate* an agreement. The blacklist can be uploaded via RPC by running the following command from the 
project's root folder:

* Unix/Mac OSX: `./gradlew uploadBlacklist`
* Windows: `gradlew uploadBlacklist`

Or by running the `Upload blacklist` run configuration from IntelliJ.

You should see three messages of the form `Blacklist uploaded to node via localhost:100XX`.

## Interacting with the nodes:

You can now interact with this CorDapp via the node shell. Note that George State Bank is a blacklisted entity, and the 
`AgreementContract` will prevent it from entering into agreements with other nodes.

For example, Monogram Bank and Hiseville Deposit Bank may enter into an agreement by running the following command from 
the shell of Monogram Bank:

    start ProposeFlow agreementTxt: "A and B agree Y", counterparty: "Hiseville Deposit Bank", untrustedPartiesAttachment: "4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A"

If you now run `run vaultQuery contractStateType: net.corda.examples.attachments.state.AgreementState` on either the 
Monogram Bank or Hiseville Deposit Bank node, you should see the agreement stored:

    data: !<net.corda.examples.attachments.state.AgreementState>
      partyA: "O=Monogram Bank, L=London, C=GB"
      partyB: "O=Hiseville Deposit Bank, L=Sao Paulo, C=BR"
      txt: "A and B agree Y"
    
However, if you try to enter into an agreement with George State Bank from the shell of Monogram Bank:

    start ProposeFlow agreementTxt: "A and B agree Y", counterparty: "George State Bank", untrustedPartiesAttachment: "4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A"
    
The flow will fail and no agreement will be stored!

# To-Do

* Currently, the blacklist jar's hash is hardcoded in the contract. An improvement would be to support any jar signed 
  by a specific trusted node
