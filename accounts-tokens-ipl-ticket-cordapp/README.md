<p align="center">
  <img src="../images/workflow.png" alt="Corda" width="500">
</p>

# IPL Cricket Ticket Booking using Accounts and Tokens

This Cordapp shows how to integrate accounts and tokens. 

# Background

This sample shows you how to integrate accounts and tokens. This sample talks about a scenario where typically when the IPL Cricket season starts, BCCI (Board of Control for Cricket) starts selling tickets.
As of now there are multiple dealers whom the BCCI issues tickets and further these dealers sell tickets to their client. We are trying to simulate similar functionality maintaining the entore issuance and selling
of the tickets on Corda Platform.

# Steps to Execute

Required Nodes-

1. BCCI node
2. Bank Node
3. Dealer1 Node
4. Dealer2 Node

Accounts-

Accounts will be created by the Dealer nodes for their clients on their nodes and will be shared with the Bank and BCCI nodes.

Looking at the above diagram follow below mentioned steps to run the application.

####  Step 1

    start CreateAndShareAccountFlow accountName : dealer1 , partyToShareAccountInfoTo : BCCI

Run the above flow on the Dealer1 node. This will create an account on the Dealer1 node and share this account info with BCCI node.
partyToShareAccountInfoTo will be modified later to take in a list so that account can be shared with multiple nodes.
The above flow will create an account named dealer1 on Dealer1 node. Similarly create below accounts on Dealer1 node.

    start CreateAndShareAccountFlow  accountName : buyer1 , partyToShareAccountInfoTo : Bank
    start CreateAndShareAccountFlow  accountName : buyer2 , partyToShareAccountInfoTo : Bank
    
The above flows will craete accounts named buyer1 and buyer2 on Dealer1's node and will share this account info with the Bank node.

####  Step 2

    run vaultQuery contractStateType : com.r3.corda.lib.accounts.contracts.states.AccountInfo

Run the above query to confirm if accounts are created on Dealer1 node. Also run the above query on Bank and BCCI node to confirm if 
account info is shared with these nodes.

####  Step 3

    start IssueCashFlow accountName : buyer1 , currency : USD , amount : 20

Run the above command on the Bank node, which will issue 20 USD to buyer1 account.

####  Step 4

    run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.FungibleToken

Run the above command on Dealer1's node to confirm if 20 USD fungible tokens are created on Dealer1's node. Look at the current holder field in the output.
This should be an AnonymousParty which specifies an account.

####  Step 5

    start CreateIplTicketTokenFlow ticketTeam : MumbaiIndiansVsRajasthanRoyals
    
Run the above flow on BCCI's node. BCCI node will create base token type for the IPL Ticket for the match MumbaiIndians Vs RajasthanRoyals.

####  Step 6

    run vaultQuery contractStateType : com.template.states.IplTicket

Get the id of this token by running the above query on BCCI node. We will require it in later steps.

####  Step 7

    start IssueNonFungibleTicketFlow tokenId : 6ff7e1a3-ce37-42ed-8fbb-1d4c6df32f00, dealerAccountName : dealer1

Run the above flow on BCCI's node to issue a non fungible token based off the token type which we created in Step5. This token will be issued by the BCCI node
to dealer1 account on Dealer1 node. 

####  Step 6

    run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.NonFungibleToken

Run the above flow on Dealer1's node to confirm if the token has been issued to the dealer1 account. Take a look at the current holder it will be a key
representing the account.

####  Step 6

    start BuyIplTicketFlow buyerAccountName : cust1 ,costOfTicket : 5 , currency : USD, 
    sellerAccountName : dealer1, tokenId : 6ff7e1a3-ce37-42ed-8fbb-1d4c6df32f00

This is the DVP flow where the buyer(buyer1 account on Dealer1 node) account will pay cash to seller account(dealer1 account on Dealer1 node), and the seller account
will transfer the ticket token to the buyer. 

####  Step 6

    run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.FungibleToken
    run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.NonFungibleToken

Confirm who owns the FungibleToken (cash) and NonFungibleToken (ticket) again by running this on Dealer1's node.


# Further Reading

For accounts visit https://github.com/corda/accounts.

For tokens visit https://github.com/corda/token-sdk.

//Add link to blog and video

# Future Enhancements

1.As of now accounts created on Dealer1 node perform the DVP. This sample will be modified to include Dealer2 as well and have accounts on Dealer1 interact with 
Dealer2's accounts. 
2.The base token type will be updated and the updates will be distributed to the current token holders.
