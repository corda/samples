<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# Contract Constraint Migration

This sample shows you how to migrate your contract constraints.

**Migrate from HashConstraint to SignatureConstraint**

1. Run the deployNodes task. I have uncommented the contract jar from deployNodes task. By default if you have contract jar in deployNodes task in nodeDefaults property, the task adds 
the jar's hash to whitelist zone param in network param. So when you issue a state, the state will be issued using whitelist zone constraint. 

        ./gradew clean deployNodes
        
2. We want to issue using hash constraint. We will explicitly add the v1-contract jar from contracts by running the below script.

        ./upgrade.sh --node=PartyA , --contract=1
        ./upgrade.sh --node=PartyB , --contract=1
    
3. Start the nodes
    
        cd build/nodes
        ./runnodes

4. Go to PartyA terminal and issue some hash constraint states

        start DefaultHashFlow counterParty : PartyB , amount : 10
        start DefaultHashFlow counterParty : PartyB , amount : 11
        start DefaultHashFlow counterParty : PartyB , amount : 12
        start DefaultHashFlow counterParty : PartyB , amount : 13
        
5. Run the vaultQuery to confirm the states have been created using HashConstraint.

        run vaultQuery contractStateType : corda.samples.upgrades.states.OldState
        
        - state:
            data: !<corda.samples.upgrades.states.OldState>
              issuer: "O=PartyA, L=Delhi, C=IN"
              owner: "O=PartyB, L=Delhi, C=IN"
              amount: 10
            contract: "corda.samples.upgrades.contracts.OldContract"
            notary: "O=Notary, L=Delhi, C=IN"
            encumbrance: null
            **constraint: !<net.corda.core.contracts.HashAttachmentConstraint>**
              attachmentId: "B66F8B2E768D5809F281160437C7C240E92E340E9128441D89E180D1A86F127E"
          ref:
            txhash: "A90143DB9EA3A7CCF3DA2E55E0E746F30A4406C82777051D8C069348008D459E"
            index: 0
        - state:
            data: !<corda.samples.upgrades.states.OldState>
              issuer: "O=PartyA, L=Delhi, C=IN"
              owner: "O=PartyB, L=Delhi, C=IN"
              amount: 11
            contract: "corda.samples.upgrades.contracts.OldContract"
            notary: "O=Notary, L=Delhi, C=IN"
            encumbrance: null
            **constraint: !<net.corda.core.contracts.HashAttachmentConstraint>**
              attachmentId: "B66F8B2E768D5809F281160437C7C240E92E340E9128441D89E180D1A86F127E"
          ref:
            txhash: "9EBED42058CB38D655561884E86EBFDC713D644BD4C103F0C61A85B2918F40E2"
            index: 0
            ...............
6. Stop the node
7. Replace the v1 contract with v2 contract. V2 contract is built using Signature Constraints.

        ./upgrade.sh --node=PartyA , --contract=2   
        ./upgrade.sh --node=PartyB , --contract=2             

8. Now disable the HashConstraint check by setting Java system property while starting each node. This disables the platform check and the new output states can be migrated to Signature Constraints.

        cd build/nodes/PartyA
        java -jar -Dnet.corda.node.disableHashConstraints="true" corda.jar
        
        cd build/nodes/PartyB
        java -jar -Dnet.corda.node.disableHashConstraints="true" corda.jar
        
9. Run the flow which consumes a HashConstraint state and creates a SignatureConstraint state.

        start MigrateToSignatureFromWhitelistFlow counterParty : PartyB , amount : 100
        
10. Run the vaultQuery to confirm the new states are using SignatureConstraints. The state issued with amount 10 is consumed and new state with amount 100 is issued
with SignatureConstraint.
    
        run vaultQuery contractStateType : corda.samples.upgrades.states.OldState
                 
        - state:
             data: !<corda.samples.upgrades.states.OldState>
               issuer: "O=PartyA, L=Delhi, C=IN"
               owner: "O=PartyB, L=Delhi, C=IN"
               amount: 11
             contract: "corda.samples.upgrades.contracts.OldContract"
             notary: "O=Notary, L=Delhi, C=IN"
             encumbrance: null
             **constraint: !<net.corda.core.contracts.HashAttachmentConstraint>**
               attachmentId: "B66F8B2E768D5809F281160437C7C240E92E340E9128441D89E180D1A86F127E"
           ref:
             txhash: "A90143DB9EA3A7CCF3DA2E55E0E746F30A4406C82777051D8C069348008D459E"
             index: 0
            index: 0
        - state:
            data: !<corda.samples.upgrades.states.OldState>
              issuer: "O=PartyA, L=Delhi, C=IN"
              owner: "O=PartyB, L=Delhi, C=IN"
              amount: 100
            contract: "corda.samples.upgrades.contracts.OldContract"
            notary: "O=Notary, L=Delhi, C=IN"
            encumbrance: null
            **constraint: !<net.corda.core.contracts.SignatureAttachmentConstraint>**
              key: "aSq9DsNNvGhYxYyqA9wd2eduEAZ5AXWgJTbTEw3G5d2maAq8vtLE4kZHgCs5jcB1N31cx1hpsLeqG2ngSysVHqcXhbNts6SkRWDaV7xNcr6MtcbufGUchxredBb6"
          ref:
            txhash: "0CEBAEA3D5B02F827FAC9445702FE5D34E78F72D6277D80195D70F92DD61A3AF"
            index: 0
        

**Migrate from WhiteListZoneConstraints to SignatureConstraint**

1. Run the deployNodes task by uncommenting v1-contract in node_defaults property. This will whitelist v1 contract and when we issue a state , it will default to whitelist zone list constraint.

        ./gradlew deployNodes 

2. Check the network param file output when you run above deployNodes command. B66F8B2E768D5809F281160437C7C240E92E340E9128441D89E180D1A86F127E is the hash of v1-contract added to the whitelist param.

        Loading existing network parameters... NetworkParameters {
              minimumPlatformVersion=5
              notaries=[NotaryInfo(identity=O=Notary, L=Delhi, C=IN, validating=false)]
              maxMessageSize=10485760
              maxTransactionSize=524288000
              whitelistedContractImplementations {
                **corda.samples.upgrades.contracts.OldContract=[B66F8B2E768D5809F281160437C7C240E92E340E9128441D89E180D1A86F127E]**
              }
              eventHorizon=PT720H
              packageOwnership {
                
              }
              modifiedTime=2019-11-07T13:44:36.945Z
              epoch=1
          }
    
3. Start the nodes
    
        cd build/nodes
        ./runnodes

4. Go to PartyA terminal and issue some states, this will default to WhiteListZoneConstraint.

        start DefaultHashFlow counterParty : PartyB , amount : 10
        start DefaultHashFlow counterParty : PartyB , amount : 11
        start DefaultHashFlow counterParty : PartyB , amount : 12
        start DefaultHashFlow counterParty : PartyB , amount : 13
 
5. Run the vaultQuery to confirm the states have been created using WhiteListZoneConstraint.
        
        run vaultQuery contractStateType : corda.samples.upgrades.states.OldState
        states:
        - state:
            data: !<corda.samples.upgrades.states.OldState>
              issuer: "O=PartyA, L=Delhi, C=IN"
              owner: "O=PartyB, L=Delhi, C=IN"
              amount: 10
            contract: "corda.samples.upgrades.contracts.OldContract"
            notary: "O=Notary, L=Delhi, C=IN"
            encumbrance: null
            constraint: !<net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint> {}
          ref:
            txhash: "01C4B2CFB31B6DE14AF20A0BABAC136E8B0E2A0DB99DC82F3AD27B6EC56FCD7F"
            index: 0
        - state:
            data: !<include_whitelist.txt>
              issuer: "O=PartyA, L=Delhi, C=IN"
              owner: "O=PartyB, L=Delhi, C=IN"
              amount: 11
            contract: "corda.samples.upgrades.contracts.OldContract"
            notary: "O=Notary, L=Delhi, C=IN"
            encumbrance: null
            constraint: !<net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint> {}
          ref:
            txhash: "EE676193242B5BA7245CDC6CCD8F84513B49D2821F3ADC651503B85B71DBAF06"
            index: 0
            ................
            
6. Stop the node

7. Replace the v1 contract with v2 contract. V2 contract is built using Signature Constraints.

        ./upgrade.sh --node=PartyA , --contract=2 
        ./upgrade.sh --node=PartyB , --contract=2             

8. Add the new jar to whitelistedContractImplementations to network param file. To do this add the network-bootstrapper, v3-contract.jar to nodes directory. Also create a file name
include_whitelist.txt and add the contract class full name to this file. 
        
        vi include_whitelist.txt
        add corda.samples.upgrades.states.OldState to include_whitelist.txt
        
        cp network-bootstrapper.jar /nodes
        cp include_whitelist.txt /nodes
        cp v3-contract.jar /nodes
        
9. Running below command will whitelist the new contract.

    java -jar network-bootstrapper.jar --dir . 
    
10. This adds the new hash of new jar to whitelistedContractImplementations to network-param file

        Updated NetworkParameters {
              minimumPlatformVersion=5
              notaries=[NotaryInfo(identity=O=Notary, L=Delhi, C=IN, validating=false)]
              maxMessageSize=10485760
              maxTransactionSize=524288000
              whitelistedContractImplementations {
                **corda.samples.upgrades.contracts.OldContract=[B66F8B2E768D5809F281160437C7C240E92E340E9128441D89E180D1A86F127E, 26973C032E20F57E62CCC7A5F0EA883E21619D433B706A743D5AD08806B7924E]**
              }
              eventHorizon=PT720H
              packageOwnership {
                
              }
              modifiedTime=2019-11-07T15:04:38.840Z
              epoch=2
          }
