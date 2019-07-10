<p align="center">
  <img src="https://cdn-images-1.medium.com/max/686/1*0r-5_TXxvBViN2G68VoVPQ@2x.png" alt="Corda" width="500">
</p>

# NonFungible House Token DvP Sample CorDapp - Java

This CorDapp servers a basic example to create, issue and perform a DvP (Delivery vs Payment) of an Evolvable NonFungible token in Corda utilizing the TokenSDK. 


# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes

### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.
    
    Tue Nov 06 11:58:13 GMT 2018>>>

You can use this shell to interact with your node.

First go to the shell of PartyA and issue some USD to Party C. We will need the fiat currency to exchange it for the house token. 

    start FiatCurrencyIssueFlow currency: USD, amount: 100000000, recipient: PartyC

We can now go to the shell of PartyC and check the amount of USD issued. Since fiat currency is a fungible token we can query the vault for FungibleToken states.

    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    
Once we have the USD issued to PartyC, we can Create and Issue the HouseToken to PartyB. Goto PartyA's shell to create and issue the house token.
    
    start HouseTokenCreateAndIssueFlow owner: PartyB, valuation: 100000 USD, noOfBedRooms: 2, constructionArea: 1000sqft, additionInfo: NA, address: Mumbai
    
We can now check the issued house token in PartyB's vault. Since we issued it as a non-fungible token we can query the vault for non-fungible tokens.
    
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
    
Note that HouseState token is an evolvable token which is a linear state, thus we can check PartyB's vault to view the evolvable token

    run vaultQuery contractStateType: corda.tokenSDK.samples.states.HouseState
    
Note the linearId of the HouseState token from the previous query, we will need it to perform our DvP opearation. Goto PartyB's shell to initiate the token sale.
    
    start HouseSaleInitiatorFlow houseId: cad35ab4-bcdb-4efd-8c63-d08fbac236fb, buyer: PartyC    
    
We could now verify that the non-fungible token has been transferred to PartyC and some 100,000 USD from PartyC's vault has been transferred to PartyB. Run the below commands in PartyB and PartyC's shell to verify the same
    
    // Run on PartyB's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    // Run on PartyC's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
      
Since our house is an evolvable token, we should be able to update the properties of our house. To update the valuation of the house token go to PartyA's shell and start the UpdateHouseValuationFlow

    start UpdateHouseValuationFlow houseId: cad35ab4-bcdb-4efd-8c63-d08fbac236fb, newValuation: 100000 USD