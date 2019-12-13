<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Negotiation CorDapp

This CorDapp shows how multi-party negotiation is handled on the Corda ledger, in the absence of an API for user 
interaction.

A flow is provided that allows a node to propose a trade to a counterparty. The counterparty has two options:

* Accepting the proposal, converting the `ProposalState` into a `TradeState` with identical attributes
* Modifying the proposal, consuming the existing `ProposalState` and replacing it with a new `ProposalState` for a new 
  amount

Only the recipient of the proposal has the ability to accept it or modify it. If the sender of the proposal tries to 
accept or modify the proposal, this attempt will be rejected automatically at the flow level.

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes:

We will interact with this CorDapp via the nodes' CRaSH shells.
  
First, go the the shell of PartyA, and propose a deal with yourself as buyer and a value of 10 to PartyB:

    flow start ProposalFlow$Initiator isBuyer: true, amount: 10, counterparty: PartyB
    
We can now look at the proposals in the PartyA's vault:

    run vaultQuery contractStateType: negotiation.states.ProposalState
    
If we note down the state's `linearId.id`, we can now modify the proposal from the shell of PartyB by running:

    flow start ModificationFlow$Initiator proposalId: <YOUR-NEWLY-GENERATED-PROPOSAL-ID>, newAmount: 8
    
Finally, let's have PartyA accept the proposal:

    flow start AcceptanceFlow$Initiator proposalId: <YOUR-NEWLY-GENERATED-PROPOSAL-ID>
    
We can now see the accepted trade in our vault with the new value by running the command (note we are now querying for 
`TradeState`s, not `ProposalState`s):

    run vaultQuery contractStateType: negotiation.states.TradeState
