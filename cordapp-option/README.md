<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Option CorDapp

This CorDapp allows nodes to issue, trade and exercise call and put options.

When issuing or trading an option, an oracle is used to ensure that the option is being exchanged for the correct 
amount of cash, based on the oracle's knowledge of stock prices, volatility and the Black-Scholes model.

The CorDapp is split into three modules:

* Client: The flows required by non-oracle nodes to query the oracle and request their signature over a transaction 
  including oracle data. Also includes a web front-end and a flow to self-issue cash
* Oracle: The flows and services required by oracle nodes to respond to data and signing queries
* Base: A collection of files that non-oracle and oracle nodes need to share, such as contract and state definitions

There is also a series of tests under `src/`.

The project is structured in this way so that non-oracle nodes only have to run the non-oracle flows and the web API, 
while oracle nodes only have to run the oracle flows and services.

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes:

You should interact with this CorDapp using the web front-end. Each node exposes this front-end on a different address:

* Issuer: `localhost:10007/web/option`
* PartyA: `localhost:10010/web/option`
* PartyB: `localhost:10013/web/option`

When using the front-end:

1. Start by issuing yourself some cash using the `Issue cash` button (the sky's the limit - about 1,000 should do)
2. Request the issuance of an option from another node using the `Request option` button
    * The issuer will sell you the option requested at it's fair value calculated using Black-Scholes
3. Hit the refresh button (either the one on the web UI or your browser's) and you should see a list of your options
   and remaining cash
4. You can now choose to:
    * Trade the option with another node for cash, by clicking the `Exercise` button next to the option. The option 
      will disappear (assuming the other node already has enough cash to pay for it!). If you visit the front-end of 
      the node that you traded the option with, you will now see it listed there
    * Exercise the option. This will lock in the exercise date, allowing you to redeem it with the issuer at a later 
      date (this functionality is not implemented yet, but would only require an additional flow and piece of contract 
      logic)

# To-Do

* Exercised options cannot yet be redeemed for cash from the issuer
* Only five stocks are supported, and the exercise date is hardcoded
* Once exercised, the `Trade` button is still displayed next to a node's options
* The contract tests are focused on the happy-path
