<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# The Upgrade CorDapp

This directory contains a number of versions of the same CorDapp (based heavily on the Obligation sample CorDapp). It is
designed to showcase ways in which a CorDapp might be upgraded such that the app remains backwards compatible between
versions.

The CorDapp is divided into contracts and workflows. These are versioned separately.

# Current versions

The CorDapp contract and workflow versions are described below.

## Contracts

### Version 1

The initial contract version.

### Version 2

This second version adds a new field to the obligation state that allows the borrower to default on the obligation. A
command is added to the contract to facilitate this. The new field has to be nullable in order to ensure backwards
compatibility between nodes with the old contract version and nodes with the new contract version while a rolling
upgrade is carried out.

## Workflows

### Version 1
The initial version of the CorDapp. This is the Obligation CorDapp using the version of FinalityFlow that was present in
Corda 3.

Compiled against version 1 of the contracts CorDapp.

### Version 2
This version of the CorDapp upgrades to use the new version of the FinalityFlow present in Corda 4. To do this such that
 the app remains compatible with Version 1, the following must be done:
 - The flows using the new FinalityFlow must be versioned
 - The flows must detect if the counterparty is using the old version, and revert to the old API accordingly
 - The app must continue to use targetPlatformVersion = 3. This is because upgrading to targetPlatformVersion = 4 results
   in the old FinalityFlow APIs becoming unusable.
   
Compiled against version 1 of the contracts CorDapp.
   
### Version 3
This version of the workflows CorDapp upgrades to using `targetPlatformVersion = 4`. As a result, the old version of
FinalityFlow cannot be used, and so this is removed. (Note that in future versions of Corda, it will be possible to set
`targetPlatformVersion = 4` and still be backwards compatible with the old FinalityFlow API. This will make this version
of the app redundant.)

Compiled against version 1 of the contracts CorDapp.

### Version 4
This version adds the flows required to default on an Obligation.

Compiled against version 2 of the contracts CorDapp.

# Upgrade scenarios

This section described the upgrade scenarios that can be explored using the workflows and contracts in this CorDapp.
Note that all commands should be run from the root of the CorDapp directory structure (i.e. the location of this Readme).

## Scenario 1: Finality Flow Upgrade

This scenario covers upgrading an app to run on V4 by upgrading its usage of FinalityFlow to the new API defined in
Corda 4. It utilises the flow versioning API to determine what version of the flow is running on the counterparty and
respond accordingly. This process can be generalised to any flow upgrade procedure that may be required.

 1. Deploy the nodes in their initial configuration by running `./gradlew deployNodes`, then `build/nodes/runnodes`.
 This creates a network of 3 nodes, all running version 1 of the contract. PartyA and PartyB are using the original
 version of the workflows, while PartyC is using version 2.
 
 2. Check that all the nodes can transact with each other. This can be done manually from the shell, or by running
 `./gradlew issueBetweenNodes`. This will issue an obligation between each pair of nodes in the network.
 
 3. Now shut down the nodes and upgrade all nodes to version 2 of the workflows CorDapp. The upgrade part of this can be
 carried out by running `scripts/upgradeNodes.sh workflows v2-finality-intermediate PartyA PartyB`.
 This simply copies the workflow jars (and any additional configuration) to the cordapps/ directory on the nodes specified.
 
 4. Restart the nodes using `build/nodes/runnodes`. All nodes should now be running V2 of the workflows CorDapp. The nodes
 should still be able to transact with each other. New obligations can again be issued between all nodes using 
 `./gradlew issueBetweenNodes`, and old obligations can be settled using `./gradlew settleAllObligations`. The shell can
 also be used to experiment.
 
 5. Shut down the nodes again, and upgrade some nodes to use V3 of the workflows CorDapp. Again, this can be carried out
 by running `scripts/upgradeNodes.sh workflows v3-finality-final PartyB PartyC`. Note that
 this upgrade can only be carried out once all the nodes in the network have been migrated to V2 of the workflows
 CorDapp, as this version of the workflows cannot use the old FinalityFlow API.
 
 6. Restart the nodes once more (`build/nodes/runnodes`). Again, all nodes should be able to transact with each other,
 either using the shell or with the gradle commands described in steps 2 and 4.
 
To see how a FinalityFlow upgrade could go wrong, instead try upgrading PartyB and C to V3 in step 3 and leaving PartyA
on V1. Once the nodes are restarted, if a transaction is attempted between PartyA and PartyB or C, a failure will be
seen. (`./gradlew issueBetweenNodes` will demonstrate this.)

## Scenario 2: Contract Version Upgrade

This section describes how to carry out an implicit contract upgrade. The upgraded contract in this case adds a new
field to the obligation state (a defaulted flag), and a new command to govern transactions that set it. Additionally,
version 4 of the workflows CorDapp provides a flow that can be used to set this flag.

1. Deploy the nodes in their initial configuration by running `./gradlew deployNodes`, then run the following:
`scripts/upgradeNodes.sh workflows v2-finality-intermediate PartyA PartyB`. This copies the V2 workflows CorDapp to PartyA and PartyB. Start the network by
 running `build/nodes/runnodes` and observe that all nodes can transact with each other (this can be done by running
 `./gradlew issueBetweenNodes`, which will issue an obligation between each pair of nodes in the network).
 
2. Shut down the network, then upgrade some of the nodes to the new version of the contract. This also requires
 upgrading to the latest version of the workflows CorDapp, as older versions are compiled against the initial contract.
 The upgrade can be carried out by running `scripts/upgradeNodes.sh workflows v4-with-default PartyA PartyB`
 and `scripts/upgradeNodes.sh contracts v2-can-default PartyA PartyB`. Note: There is some additional configuration for the nodes running the new versions of the workflows CorDapp. This
 configuration is used to control whether the new field in the contract state is set when the state is created. The no
 downgrade rule will prevent nodes with only the old contract version from using the state if it has new fields set, and
 so as a result the new field cannot be set until all nodes in the network are upgraded. To begin with, this configuration
 prevents the field from being populated.
 
3. Run the network (`build/nodes/runnodes`). In order to issue obligations between nodes, the contract attachment must be
 present in the attachment store of each node (if this isn't the case, then an error will be hit when sending a transaction
 with a version of the contract the node doesn't recognise). To put a copy of each contract version in the network in each
 node's attachment store, run `./gradlew loadAttachments`.
 
 4. Check that obligations can be issued between nodes (again, `./gradlew issueBetweenNodes` can be used). It is
 instructive to show that obligations can still be freely transferred between nodes running different contract versions
 at this point. To do this, first clear all obligations from the nodes (the easiest way to do this is to settle them all
 using `./gradlew settleAllObligations`), then do the following:
  
   - Issue an obligation from the shell of PartyB, naming PartyA as the lender (`start IssueObligation$Initiator amount:
   $100, lender: PartyA, anonymous: true`)
   - On PartyA, do `run vaultQuery contractStateType: net.corda.examples.obligation.contract.Obligation` and note the
   linear ID of the obligation state.
   - Transfer the obligation to PartyC: `start TransferObligation$Initiator newLender: PartyC, linearId: <linear ID>, anonymous: true`
   - Transfer the obligation back, by running the same command on PartyC and naming PartyA as the new lender.
  
5. Shut down the network, then upgrade the final node (PartyC) to use the new contract:
   - `scripts/upgradeNodes.sh workflows v4-can-default PartyC`
   - `scripts/upgradeNodes.sh contracts v2-with-default PartyC`
  
6. Adjust the CorDapp configuration to set the new field in the obligation state. (Note that in a real system, this
 would be done with a new version of the workflows CorDapp that starts to populate the field.) To do this, go to each
 node's CorDapp configuration (build/nodes/<node>/cordapps/config/obligation-workflows.conf) and set the only field in
 that file to true.
 
7. Restart the nodes, and try out the commands in step 3 again. Note that the "defaulted" field in the obligation state
 is set to "false" (rather than null) when querying obligation states using a vault query.
 
8. Try the new defaulting flow, by running `start DefaultObligation$Initiator linearId: <linear ID>` from the borrower
 of the obligation. This will set the field to true (and nothing else).
 
To see some of the ways this could be set up incorrectly:
 - Try also setting the configuration option to true for the nodes running the new CorDapp version in step 3. This will
 prevent the node running the old version (PartyC) from spending the obligation state due to the no downgrade rule. The
 node will accept the transferred obligation from PartyA, but as there is information in the new field and this would
 be lost, the transaction to send it back will fail. This means that nodes cannot start using new fields in contract
 state definitions until all nodes in the network have been upgraded, unless it is acceptable to leave nodes with
 unspendable states.
 - To fix the above situation, upgrade the node (follow step 4), then restart the network and try again.

# The Obligation CorDapp

This CorDapp comprises a demo of an IOU-like agreement that can be issued, transferred and settled confidentially. The CorDapp includes:

* An obligation state definition that records an amount of any currency payable from one party to another. The obligation state
* A contract that facilitates the verification of issuance, transfer (from one lender to another) and settlement of obligations
* Three sets of flows for issuing, transferring and settling obligations. They work with both confidential and non-confidential obligations

The CorDapp allows you to issue, transfer (from old lender to new lender) and settle (with cash) obligations. It also 
comes with an API and website that allows you to do all of the aforementioned things.

# Instructions for setting up

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.
