<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Fungible and NonFungible RealEstate Token Sample CorDapp - Java

This cordapp servers as a basic example to create, issue, move fungible tokens in Corda utilizing the TokenSDK. In this specific fungible token sample, we will not talk about the
redeem method of the TokenSDK because the redeem process will take the physical asset off the ledger and destroy the token. Thus, this sample will be a simple walk though of the
creation, issuance, and transfer of the tokens.



# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

For a brief introduction to Token SDK in Corda, see https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025

# Usage

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes

### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.

### Fungible Tokens

Create house on the ledger using Seller's terminal

    flow start CreateHouseTokenFlow symbol: house, valuation: 100000

This will create a linear state of type HouseTokenState in Seller's vault

Seller will now issue some tokens to Buyer. run below command via Seller's terminal.

    flow start IssueHouseTokenFlow symbol: house, quantity: 50, holder: Buyer

Now at Buyer's terminal, we can check the tokens by running:
    flow start GetTokenBalance symbol: house

Since Buyer now has 50 tokens, Move tokens to Friend from Buyer s terminal

    flow start MoveHouseTokenFlow symbol: house, holder: Friend, quantity: 23
