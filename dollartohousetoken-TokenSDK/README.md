<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# NonFungible House Token DvP Sample CorDapp - Java

This CorDapp servers a basic example to create, issue and perform a DvP (Delivery vs Payment) of an Evolvable NonFungible token in Corda utilizing the TokenSDK. 


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

First go to the shell of PartyA and issue some USD to Party C. We will need the fiat currency to exchange it for the house token. 

    start FiatCurrencyIssueFlow currency: USD, amount: 100000000, recipient: PartyC

We can now go to the shell of PartyC and check the amount of USD issued. Since fiat currency is a fungible token we can query the vault for FungibleToken states.

    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    
Once we have the USD issued to PartyC, we can Create and Issue the HouseToken to PartyB. Goto PartyA's shell to create and issue the house token.
    
    start HouseTokenCreateAndIssueFlow owner: PartyB, valuation: 10000 USD, noOfBedRooms: 2, constructionArea: 1000sqft, additionInfo: NA, address: Mumbai
    
We can now check the issued house token in PartyB's vault. Since we issued it as a non-fungible token we can query the vault for non-fungible tokens.
    
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
    
Note that HouseState token is an evolvable token which is a linear state, thus we can check PartyB's vault to view the evolvable token

    run vaultQuery contractStateType: net.corda.examples.dollartohousetoken.states.HouseState
    
Note the linearId of the HouseState token from the previous step, we will need it to perform our DvP opearation. Goto PartyB's shell to initiate the token sale.
    
    start HouseSaleInitiatorFlow houseId: <XXXX-XXXX-XXXX-XXXXX>, buyer: PartyC
    
We could now verify that the non-fungible token has been transferred to PartyC and some 100,000 USD from PartyC's vault has been transferred to PartyB. Run the below commands in PartyB and PartyC's shell to verify the same
    
    // Run on PartyB's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    // Run on PartyC's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
