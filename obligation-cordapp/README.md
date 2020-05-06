<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# The Obligation CorDapp

This CorDapp comprises a demo of an IOU-like agreement that can be issued, transfered and settled confidentially. The CorDapp includes:

* An obligation state definition that records an amount of any currency payable from one party to another. The obligation state
* A contract that facilitates the verification of issuance, transfer (from one lender to another) and settlement of obligations
* Three sets of flows for issuing, transferring and settling obligations. They work with both confidential and non-confidential obligations

The CorDapp allows you to issue, transfer (from old lender to new lender) and settle (with cash) obligations. It also 
comes with an API and website that allows you to do all of the aforementioned things.

# Instructions for setting up

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

# Using the CorDapp via the web front-end

In your favourite web browser, navigate to:

1. PartyA: `http://localhost:10007`
2. PartyB: `http://localhost:10010`
3. PartyC: `http://localhost:10013`

You'll see a basic page, listing all the API end-points and static web content. Click on the "obligation" link under 
"static web content". The dashboard shows you a number of things:

1. All issued obligations to date
2. A button to issue a new obligation
3. A button to self issue cash (used to settle obligations)
4. A refresh button

## Issue an obligation

1. Click on the "create IOU" button.
2. Select the counterparty, enter in the currency (GBP) and the amount, 1000
3. Click create IOU
4. Wait for the transaction confirmation
5. Click anywhere
6. The UI should update to reflect the new obligation.
7. Navigate to the counterparties dashboard. You should see the same obligation there. The party names show up as random public keys as they are issued confidentially. Currently the web API doesn't resolve the party names.

## Self issue some cash

From the obligation borrowers UI:

1. Click the issue cash button
2. Enter a currency (GBP) and amount, 10000
3. Click "issue cash"
4. Wait for the transaction confirmation
5. click anywhere
6. You'll see the "Cash balances" section update

## Settling an obligation

In order to complete this step the borrower node should have some cash. See the previous step how to issue cash on the borrower's node.

From the obligation borrowers UI:

1. Click the "Settle" button for the obligation you previously just issued.
2. Enter in a currency (GBP) and amount, 500
3. Press the "settle" button
4. Wait for the confirmation
5. Click anywhere
6. You'll see that £500 of the obligation has been paid down
7. Navigate to the lenders UI, click refresh, you'll see that £500 has been paid down

This is a partial settlement. you can fully settle by sending another £500. The settlement happens via atomic DvP. The obligation is updated at the same time the cash is transfered from the borrower to the lender. Either both the obligation is updated and the cash is transferred or neither happen.

That's it!

From the lenders UI you can transfer an obligation to a new lender. The procedure is straight-forward. Just select the Party which is to be the new lender.


# TODO

1. Resolve party names for the web front-end.
2. Replace the Corda web server with a reactive spring boot web server


Feel free to submit a PR.
