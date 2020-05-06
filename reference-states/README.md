

<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Reference State Cordapp

This CorDapp demonstrates the use of reference states in a transaction and in the verification method of a contract.

This CorDapp allows two nodes to enter into an IOU agreement, but enforces that both parties belong to a list of sanctioned entities. This list of sanctioned entities is taken from a referenced SanctionedEntities state.


## Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

## Usage

### Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

### Running the flow

We will interact with the nodes via their shell. 

When the nodes are up and running, the first thing you need to do is create a sanctions list. To do this, open the shell for the SanctionsBody node and run the command:
   
    flow start IssueSanctionsListFlow
   
Now that the sanctions list has been made, the party that wants to issue the flow needs to be able to reference it. To do this, it needs to pull the sanction list into its own vault. From the IOUPartyA shell run:
    
    flow start GetSanctionsListFlow otherParty: SanctionsBody

Next, we want to issue an IOU. Run from the IOUPartyA shell:
    
    flow start IOUIssueFlow iouValue: 5, otherParty: IOUPartyB, sanctionsBody: SanctionsBody
     
We've seen how to successfully send an IOU to a non-sanctioned party, so what if we want to send one to a sanctioned party? First we need to update the sanction list so, from the SanctionsParty shell, run:

    flow start UpdateSanctionsListFlow partyToSanction: DodgyParty
    
We need to update the reference before we use it in a new transaction so, from IOUPartyA's shell, run:
    
    flow start GetSanctionsListFlow otherParty: SanctionsBody
    
Now try an issue a flow to DodgyParty:

    flow start IOUIssueFlow iouValue: 5, otherParty: DodgyParty, sanctionsBody: SanctionsBody
    
The flow will error with the message 'java.lang.IllegalArgumentException: Failed requirement: The borrower O=DodgyParty, L=Moscow, C=RU is a sanctioned entity'!

