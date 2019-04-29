<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# The Implicit Upgrades CorDapp

This CorDapp shows how to carry out implicit upgrades of a CorDapp, by writing flows and contracts in a backwards compatible manner and taking advantage of Signature Constraints. For an example of upgrading a contract using the Contract Upgrade Flow, see the Explicit Contract Upgrades app (https://github.com/corda/samples/tree/release-V4/explicit-contract-upgrades).

This directory contains a number of versions of the same CorDapp (based heavily on the Obligation sample CorDapp here:
https://github.com/corda/samples/tree/release-V4/obligation-cordapp). It is
designed to showcase ways in which a CorDapp might be upgraded such that the app remains backwards compatible between
versions.

The CorDapp is divided into contracts and workflows as per the recommendations here:
https://docs.corda.net/head/versioning.html#publishing-versions-in-your-jar-manifests.

# Current versions

The CorDapp contract and workflow versions are described below.

## Contracts

### Version 1

The initial contract version.

### Version 2

This second version adds a new field to the obligation state that allows the borrower to default on the obligation. A
command is added to the contract to facilitate this. The new field has to be nullable in order to ensure backwards
compatibility between nodes with the old contract version and nodes with the new contract version - if this field were
not nullable, it would be impossible to spend states created with the old contract version.

This last point deserves a little clarification. It must be possible for nodes running the new version of the contract
to deserialize old states. However, for this to work, the new contract code has to choose a value for the new property,
that previously had no value stored. This can be done by making the property nullable (in which case null will be chosen
for old states).

Additionally, note that if a new state is sent to a node running the old version, and the new property contains any data,
then the old node will be unable to deserialize it and hence spend it. This is the no-downgrade rule, which prevents
data from accidentally being thrown away due to a change in contract version.

See https://docs.corda.net/head/serialization.html#custom-types for a full explanation around the rules for
serializing states.

(TODO: add a link to another documentation page, when written, better describing contract upgrades.)

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
 
See https://docs.corda.net/head/app-upgrade-notes.html#step-5-security-upgrade-your-use-of-finalityflow for a
walkthrough for upgrading other CorDapps.
   
Compiled against version 1 of the contracts CorDapp.

### Version 3
This version adds the flows required to default on an Obligation.

Compiled against version 2 of the contracts CorDapp.

# Upgrade scenarios

This section describes the upgrade scenarios that can be explored using the workflows and contracts in this CorDapp.
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
 carried out by running `scripts/upgradeNodes.sh workflows v2-new-finality-flow PartyA PartyB`.
 This copies the workflow jars to the cordapps/ directory on the nodes specified.
 
 4. Restart the nodes using `build/nodes/runnodes`. All nodes should now be running V2 of the workflows CorDapp. The nodes
 should still be able to transact with each other. New obligations can again be issued between all nodes using 
 `./gradlew issueBetweenNodes`, and old obligations can be settled using `./gradlew settleAllObligations`. The shell can
 also be used to experiment.
 
Note: If this example is compiled against Corda 4.0, then a bug will prevent this scenario from executing successfully.
By default, the sample is compiled against a snapshot of 5.0, which does not have the problem.
The issue is that the new version of `FinalityFlow` in 4.0 has a `targetPlatformVersion` gate that prevents the old API
from being called if the app has set `targetPlatformVersion = 4`. To fix this, change the V2 workflows app to use
`targetPlatformVersion = 3` instead. This issue is fixed in Corda 4.1.

## Scenario 2: Contract Version Upgrade

This section describes how to carry out an implicit contract upgrade. The upgraded contract in this case adds a new
field to the obligation state (a defaulted flag), and a new command to govern transactions that set it. Additionally,
version 4 of the workflows CorDapp provides a flow that can be used to set this flag.

1. Deploy the nodes in their initial configuration by running `./gradlew deployNodes`. Start the network by
 running `build/nodes/runnodes` and observe that all nodes can transact with each other (this can be done by running
 `./gradlew issueBetweenNodes`, which will issue an obligation between each pair of nodes in the network).
 
2. Shut down the network, then upgrade two of the nodes to the new version of the contract. This also requires
 upgrading the same nodes to the latest version of the workflows CorDapp, as older versions are compiled against the initial contract.
 The upgrade can be carried out by running `scripts/upgradeNodes.sh workflows v3-can-default PartyA PartyB`
 and `scripts/upgradeNodes.sh contracts v2-obligation-can-default PartyA PartyB`.
 
3. Run the network (`build/nodes/runnodes`). First, check that the two nodes with upgraded contracts can transact between
themselves. On PartyA's shell, try `start IssueObligation amount: $100, lender: PartyB, anonymous: true`. This should
succeed. It should also be possible for PartyC to issue an obligation using the old version of the contract, while
naming an upgraded node as a lender. From PartyC's shell, try `start IssueObligation amount: $100, lender: PartyA, anonymous: true`
 
4. Now demonstrate that a party using the old version of the contract cannot accept a transaction with a state on a newer
version of the contract. From the shell of PartyB, run `start IssueObligation amount: $100, lender: PartyC, anonymous: true`.
This will fail on PartyC, as it receives a state that it cannot deserialize. To fix this problem, the node needs to be
upgraded to the latest version of the contract.
  
5. Shut down the network, then upgrade the final node (PartyC) to use the new contract:
   - `scripts/upgradeNodes.sh workflows v3-can-default PartyC`
   - `scripts/upgradeNodes.sh contracts v2-obligation-can-default PartyC`
 
6. Restart the nodes. It should now be possible for every node to transact with all the others. This can either be
demonstrated by running the same commands from the shell in step 3, or by running `./gradlew issueBetweenNodes` (which will
issue an obligation between each pair of nodes).
 
7. Try the new defaulting flow. Pick a node and do `run vaultQuery contractStateType: net.corda.examples.obligation.contract.Obligation` to find a
 linear ID of an obligation for which the node is the borrower. (An easy way of doing this is to settle all obligations
 by running `./gradlew settleAllObligations`, and then issuing an obligation from the node. There will be a single
 obligation in the vault, and this node will be the borrower). Once the linear ID is obtained, run
 `start DefaultObligation$Initiator linearId: <linear ID>`. This will set the field to true (and nothing else).

