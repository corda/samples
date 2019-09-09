<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# Flow Hospital Example

This sample demonstrates two scenarios which shows how the flows get checkpointed in the node checkpoint table when a flow throws an error.
We will see how the checkpointed flow is retried everytime we restart the node.

Background

A flow is checkpointed at various points(at creation, while sending or receiving a message to/from the counterparty).
A flow is also checkpointed when the flow throws and error. At this point an interceptor incepts the error and send its to the flow hospital.
The error is diagnosed by the FlowHospital and depending on the diagnosis the flow is either - 
1. the flow is checkpointed and kept in the database for manual intervention. The flow is retried everytime the node is restarted. If the flow completes its entry is removed from
the flow checkpoint table otherwise the flow remains in the table. You should always drain (complete the pending flows in the node checkpoint table or remove these 
flows by calling killflow) the node before upgrading your flows.
2. the flow is retried for x times as configured (if its an SQLException like a deadlock the flow is retried for a maximum of 10 times)
3. error is propogated to the relevant parties and the checkpoint is not persisted in the db.

Checkpoint Handling

A checkpointed flow is retried when a node starts and if successful and is removed from the database. If not successful, this checkpoint should be manually
removed from the db before performing a flow upgrade. This process is called as flow draining. Flow draining lets the flows completes and removes them 
from the database. If still the flow is not removed from the database, we can use the killFlow RPC command to kill/remove the flow.

We will be using 2 versions for the flows.

**Flow 1**
In this version, only the TenderingOrganisation will sign the transaction.

**Flow2**
In this version, TenderingOrganisation and the bidder will sign the transaction. This calls the subflow CollectSignaturesFlow to collect signatures from
counterparty.

## Scenario 1: Tendering Organisation on new Flow, PartyA on old flow. Flow is checkpointed on PartyA's side.

**Step1:** 

Deploy version1 of contracts and flows by running `./gradlew deployNodes`. Upgrade the flow of TenderingOrganisation to flow 2 by running
    `cd script`
    `./upgrade.sh --node=TenderingOrganisation --workflow=2`.
Now check cordapps folder, PartyA will have flow1 and  TenderingOrganisation will have flow2 jars. 
Now run the nodes using `./build/nodes/runnodes`. 
This would create a network of 3 nodes and a notary.

**Step2:** 

Run the CreateAndPublishTenderFlow. CreateAndPublishTenderFlow takes in the tenderName and the bidder to whom the tender has to be published.
Run the below command from TenderingOrganisation's terminal.

    start CreateAndPublishTenderFlow tenderName : tenderForRoad , bidder1 : PartyA

TenderingOrganisation calls CollectSignaturesFlow which internally calls subflow SendTransactionFlow. SendTransactionFlow requires ReceiveTransactionFlow
at the other side. SignTransactionFlow internally calls ReceiveTransactionFlow. But since PartyA has older version which doesnt have the SignTransactionFlow
called at the responder, the TenderingOrganisation waits for its reply forever after calling a sendAndReceive from SendTransactionFlow.
Meanwhile PartyA moves ahead and calls ReceiveFinalityFlow which fails saying SignaturesMissingException.
The flow at TenderingOrganisation hangs  because the sequence of send and receive if not proper.
The flow is checkpointed at PartyA. 
Restarting the node/draining the node will not solve the problem. 
We will kill the flow at PartyB by calling killFlow RPC command.

Run the below command to get the list of checkpoints.

    >>run stateMachinesSnapshot
        id: "1f37c693-ed1b-4810-8158-d9ef41cb9ac1"
        flowLogicClassName: "corda.samples.upgrades.flows.CreateAndPublishTenderResponderFlow"
        party: "O=TenderingOrganisation, L=Delhi, C=IN"
        invocationId:
        value: "f9b6562a-178a-421a-adda-71e27690cf26"
        timestamp: "2019-09-04T12:15:03.020Z"
        sessionId:
        value: "f9b6562a-178a-421a-adda-71e27690cf26"
        timestamp: "2019-09-04T12:15:03.020Z"

This will output the id of the flow. 
Stop the node. 
Copy the id from the stateMachinesSnapshot command and hit the killFlow RPC command to kill/remove the flow from node checkpoint table.

    run killFlow id : 1f37c693-ed1b-4810-8158-d9ef41cb9ac1   
    
Once you have killed the flow take a look at the checkpoints table again. It doesn't return anything.

    run stateMachinesSnapshot
    []

You can now upgrade PartyA flow to flow2. If you upgrade PartyA's flow to flow2 before clearing the checkpoint, the node will 
complain with below error. So upgrading the flow also doesn't fix the issue.

    ATTENTION: Found checkpoint for flow: class corda.samples.upgrades.flows.CreateAndPublishTenderResponderFlow that is incompatible with the current installed version of v2-workflows. Please reinstall the previous version of the CorDapp (with hash: 3C5BB18CECD8376D2927EF1ABE4902C89CFE8C28363AB7AB311B1DACC4C81C31), drain your node (see https://docs.corda.net/upgrading-cordapps.html#flow-drains), and try again.
     
## Scenario 2: Tendering Organisation on new Contract, PartyA on old contract. Flow is propogated to the terminal by FlowHospital.

**Step1:** 

Upgrade the flow of TenderingOrganisation to flow 2 by running
    `cd script`
    `./upgrade.sh --node=TenderingOrganisation --contract=2`.
Now check cordapps folder, PartyA will have contract1 and  TenderingOrganisation will have contract2 jars. 
Now run the nodes using `./build/nodes/runnodes`. 
This would create a network of 3 nodes and a notary.

**Step2:** 

Run the CreateAndPublishTenderFlow. CreateAndPublishTenderFlow takes in the tenderName and the bidder to whom the tender has to be published.
Run the below command from TenderingOrganisation's terminal.

    start CreateAndPublishTenderFlow tenderName : tenderForCement , bidder1 : PartyA

The flow throws error on TenderingOrganisation's terminal saying Counter Party Flow errored. The counterparty is still on the old contract and trusts
only contract1. It is not aware of contract2, hence throws below exception

    UntrustedAttachmentsException: Attempting to load untrusted transaction attachments.
    
The error is handled by the FlowHospital and is thrown away to the TenderingOrganisation.