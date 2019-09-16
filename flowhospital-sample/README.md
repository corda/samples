<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# Flow Hospital Example

This sample demonstrates two scenarios which shows how the flows get checkpointed in the node checkpoint table when a flow throws an error.
We will see how the checkpointed flow is retried everytime we restart the node.

Background

A flow is checkpointed at various points(at creation, while sending or receiving a message to/from the counterparty etc).
A flow is also checkpointed when the flow throws and error. At this point an interceptor incepts the error and send its to the flow hospital.
The error is diagnosed by the FlowHospital and depending on the diagnosis the flow is either - 
1. the flow is kept in the database for manual intervention. The flow is retried everytime the node is restarted. If the flow completes its entry is removed from
the flow checkpoint table otherwise the flow remains in the table. You should always drain (complete the pending flows in the node checkpoint table or remove these 
flows by calling killflow) the node before upgrading your flows.
2. the flow is retried for x times as configured (if its an SQLException like a deadlock the flow is retried for a maximum of 10 times)
3. error is propogated to the relevant parties(parties awaiting on the session by calling receive/sendAndReceive) and the checkpoint is removed from the db.

Checkpoint Handling

A checkpointed flow is retried when a node starts and if successful and is removed from the database. If not successful, this checkpoint should be manually
removed from the db before performing a flow upgrade. This process is called as flow draining. Flow draining lets the flows completes and removes them 
from the database. If still the flow is not removed from the database, we can use the killFlow RPC command to kill/remove the flow.
   
## Scenario 1: Error in ReceiveFinalityFlow triggers custom Logic in FlowHospital, and errored flows checkpoint is not cleared from the database. 

In this scenario we will see how flow hospital handles specific errors and gives the node a chance of recovery. The flow encounters error at PartyA 
because it has an old contract. Tendering Organisation has new contract.PartyA does not trust the new contract and hence fails. 
The flow succeeds on Tendering Organisation, and the new state is saved in Tendering Organisation's vault and fails to save on PartyA's vault.
The checkpointed flow on PartyA's side is tried again at every node restart. Manually intervening and upgrading PartyA to new contract will fix this issue. 
We will stop the node.Upgrade the contract to contract 2 on PartyA. Start the node again. The checkpointed flow will be tried at startup, 
it will succeed this time it has the new contract.

**Step1:** 

Upgrade the flow of TenderingOrganisation to contract 2 by running
    `cd script`
    `./upgrade.sh --node=TenderingOrganisation --contract=2`.
Now check cordapps folder, PartyA will have contract1 and  TenderingOrganisation will have contract2 jars. 
Now run the nodes using `./build/nodes/runnodes`. 
This would create a network of 3 nodes and a notary.

**Step2:** 

Run the CreateAndPublishTenderFlow. CreateAndPublishTenderFlow takes in the tenderName and the bidder to whom the tender has to be published.
Run the below command from TenderingOrganisation's terminal.

    start CreateAndPublishTenderFlow tenderName : tenderForCement , bidder1 : PartyA, tenderAmount : 200

The flow succeeds on TenderingOrganisation side. On the PartyA's side the flow is kept for overnight observation and is retried from its last safe
checkpoint at node startup. The TenderState is saved on TenderingOrganisation vault, and is not saved in PartyA's vault as it fails in ReceiveFinalityFlow
where it doesnt trust the new contract. Run the below command to check if tender state is saved in the vault.

Run below command on TenderingOrganisation terminal, which will output the above created tender state.

    run vaultQuery contractStateType : corda.samples.upgrades.states.TenderState
    
Run below command on PartyA's terminal, which will not output anything, which shows the state is not saved on PartyA's side.

    run vaultQuery contractStateType : corda.samples.upgrades.states.TenderState
    
Run the below command on PartyA's side to check the current checkpointed flows, which shows you teh checkpointed flow.

    run stateMachinesSnapshot

Shutdown only PartyA node. Upgrade the contract on PartyA's side to contract2 by running below command.

    cd script
    ./upgrade.sh --node=PartyA --contract=2.
    
Start PartyA's node. The flow is triggered from last saved safe checkpoint, the flow succeeds and the state gets saved onto PartyA's vault. 
Try running the stateMachinesSnapshot command which will not output anything now. Also try running below command to check if the state is saved on PartyA's
vault.

    run vaultQuery contractStateType : corda.samples.upgrades.states.TenderState

