<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Contract Upgrades CorDapp

This CorDapp shows the end-to-end process of upgrading contracts in Corda.

The upgrade takes place in four stages:

1. Create a replacement contract implementing the `UpgradedContract` interface
2. Bundle the replacement contract into a CorDapp and install it on each node
3. For each state you wish to upgrade the contract of, authorise the contract upgrade for that state on each node
4. On a single node, authorise the contract upgrade for each state you wish to upgrade the contract of

In this case, we are only upgrading the contract. It is also possible to upgrade the state only, or both the state and 
the contract at once.

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Upgrading the contract:

Run the following command from the project's root folder:

* Unix/Mac OSX: `./gradlew runUpgradeContractClient`
* Windows: `gradlew runUpgradeContractClient`

This will run the contract upgrade client defined here:
https://github.com/corda/contract-upgrades/blob/release-V3/cordapp/src/main/kotlin/com/upgrade/Client.kt. This
client will:

1. Connect to PartyA and PartyB's nodes via RPC
2. Issue a state with the old contract
3. Upgrade the state to use the new contract
4. Wait ten seconds for the contract upgrade to propagate
5. Log the state to show that its contract has been upgraded

You should see a message of the form:

    ```I 17:41:35 1 UpgradeContractClient.main - TransactionState(data=State(a=C=GB,L=London,O=PartyA, b=C=US,L=New 
    York,O=PartyB), contract=com.upgrade.NewContract, notary=C=GB,L=London,O=Notary,CN=corda.notary.validating,
    encumbrance=null, constraint=HashAttachmentConstraint(attachmentId=670BD1385F920D5F87FA9F42FAA2DE86E31F1CAD...))```

# To-Do

* Create an example of upgrading states and their contracts simultaneously
* Create an example of upgrading states but not their contracts
