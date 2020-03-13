# Auction CorDapp

This CorDapp serves as a demo of building an auction application on Corda. It leverages
different features of Corda like `SchedulableState`, `StatePointer` and `OwnableState`. It also demonstrate 
how to perform a DvP (Delivery vs Payment) transaction on Corda.

It has a full-functional client included and an angular UI to interact with the nodes.

## Pre-requisites:
See https://docs.corda.net/getting-set-up.html.

## Running the nodes:
 
See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Running the client:

The client can be run by executing the below command from the project root:

`./gradlew runAuctionClient`

Please make sure that the nodes are already running before starting the client. 
The client can be accessed at http://localhost:8085/

## Usage

1. Click on the "Setup Demo Data" button to create somde demo data to play with or you
may use the skip button if you wish to setup the deata yourself.
![Setup Data](./snaps/setup.png)

2. The demo data setup would have created some assets from each of the parties. The assets
can be found under MyAssets Section. These assets could be put on auction. New assets can
be create using the create asset button.
The drop down at the top right corner can be used to toggle between parties.
The balance next to it indicates the current active parties cash balance. Cash can be 
issued using the Issue Cash Button.
![Landing Page](./snaps/landing.png)

3. Click on an asset to put it on auction. Input the `Base Price` and the `Auction Deadline`
and click in the Create Auction button to Create the Auction.
![Create Auction](./snaps/CreateAuction.png)

4. Once an auction is created it would be available in the `Active Auction` section. Its
now ready to accept bids. Switch to PartyB and place a bid, by clicking on the auction 
available in the `Active Auction` section.
![Place Bid](./snaps/Bid.png)

5. Place one more bid by switching to PartyC.

6. What for the auction to end.

7. Once the auction is ended its ready to be settled. Settlement can be initiated by the 
highest bidder. Considering PartyC is the highest bidder, switch to PartyC.

8. Issue cash equivalent or greater than the highest bid amount for PartyC to pay for 
    the auction settlement.
![Issue Cash](./snaps/CashIssue.png) 

9. Now click on the auction and initiate the settlement using the `Pay and Settle` Button. 
![Pay and Settle](./snaps/Settle.png) 


Notice the below things that would happed on auction settlement:

- Auctioned Assets ownership would be transferred to the highest bidder. The asset would 
now appear in the auction winners asset list (My Assets section).
- The auctioneers cash balance would be credited with amount equivalent to the highest bid.
- The highest bidders (PartyC in this case) cash balance would also be debited with amount 
equivalent to the highest bid. 
- The AuctionState would be consumed and the auction would no longer be visible.