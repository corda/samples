<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Fungible and NonFungible RealEstate Token Sample CorDapp - Java

This cordapp servers as a basic example to create, issue, move, redeem fungible and non fungible tokens in Corda utilizing the TokenSDK.

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

Create house on the ledger using PartA's terminal

    start CreateEvolvableFungibleTokenFlow valuation : 100

This will create a linear state of type RealEstate in A's vault

Get the uuid of the house type from PartyA's terminal by hitting below command.

    run vaultQuery contractStateType : com.template.states.RealEstateEvolvableTokenType

PartyA will now issue some tokens to PartB. Fire below command via PartyA's terminal using uuid collected from previous step.

    start IssueEvolvableFungibleTokenFlow tokenId : 680922c8-a0c8-4440-9d72-7dfefb380661 , quantity : 10 , holder : PartyB

Since PartyB now has 10 tokens, Move tokens to PartyC from PartyB s terminal

    start MoveEvolvableFungibleTokenFlow tokenId : 680922c8-a0c8-4440-9d72-7dfefb380661 , holder : PartyC , quantity : 5

Redeem tokens via PartyC's terminal specifying the issuer

    start RedeemHouseFungibleTokenFlow tokenId : 680922c8-a0c8-4440-9d72-7dfefb380661 , issuer : PartyA , quantity : 5

### Non Fungible Tokens

Create house on the ledger on PartyA's terminal

    start CreateEvolvableTokenFlow valuation : 100

Issue tokens off the created house from PartyA s terminal to PartyB

    start IssueEvolvableFungibleTokenFlow tokenId : 79332247-61d2-4d00-bb1f-d62416cf4920 , quantity : 10 , holder : PartyB

Move tokens to PartyC from PartyB s terminal

    start MoveEvolvableFungibleTokenFlow tokenId : 79332247-61d2-4d00-bb1f-d62416cf4920 , holder : Party C , quantity : 5

Redeem tokens via PartyC's terminal

    start RedeemHouseFungibleTokenFlow tokenId : 79332247-61d2-4d00-bb1f-d62416cf4920, issuer : PartyA , quantity : 5
