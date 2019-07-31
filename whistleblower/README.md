<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Whistle Blower CorDapp

This CorDapp is showcase of confidential identities and *triggering a flow upon any state changes* with the use of `CordaService` and `Trackby`.

Keep in mind that confidential identities are used to sign the transactions, but they are not used for logging. The legal identities are still used for logging. Therefore, to pursuing anonymous whistle blowing, we have the following design: 


1. Node BraveEmployee(the *whistle-blower*) whistle-blows on a BadCompany to SurveyMonkey node (the *MidLayer*). 
    * Both the whistle-blower and the SurveyMonkey generate anonymous public keys for this transaction, meaning that any third-parties who manage to get ahold of the state cannot identity the whistle-blower or SurveyMonkey. This process is handled 
    * automatically by the `SwapIdentitiesFlow`. 
2. And then, SurveyMonkey automatically direct the whistle-blowing message to the investigator(Again, using confidential identities). 
3. As a result, the investigator will not see the whistle-blowers' identity from any means, and anonymous whistle blowing is achieved. 

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes:

We will interact with this CorDapp via the nodes' CRaSH shells.
  
First, go the the shell of BraveEmployee, and report BadCompany to the SurveyMonkey by running:

    flow start BlowWhistleFlow badCompany: BadCompany, investigator: SurveyMonkey
    
To see the whistle-blowing case stored on the whistle-blowing node, run:

    run vaultQuery contractStateType: com.whistleblower.BlowWhistleState

    [ {
      "badCompany" : "C=KE,L=Eldoret,O=BadCompany",
      "whistleBlower" : "8Kqd4oWdx4KQGHGKubAvzAFiUG2JjhHxM2chUs4BTHHNHnUCgf6ngCAjmCu",
      "investigator" : "8Kqd4oWdx4KQGHGGdcHPVdafymUrBvXo6KimREJhttHNhY3JVBKgTCKod1X",
      "linearId" : {
        "externalId" : null,
        "id" : "5ea06290-2dfa-4e0e-8493-a43db61404a0"
      },
      "participants" : [ "8Kqd4oWdx4KQGHGKubAvzAFiUG2JjhHxM2chUs4BTHHNHnUCgf6ngCAjmCu", "8Kqd4oWdx4KQGHGGdcHPVdafymUrBvXo6KimREJhttHNhY3JVBKgTCKod1X" ]
    } ]

And then, behind the scenes, the SurveyMonkey node will automatically pick up the Vault update and start the AutoDirectFlow to direct the original BlowWhistleState to the investigator 

We can see the whistle-blowing case stored on the investigator node, by running:

```
run vaultQuery contractStateType: com.whistleblower.AutoDirectState
```


As we can see, the whistle-blower,SurveyMonkey, and investigator are identified solely by anonymous public keys. 


# Flow Triggering using CordaService and TrackBy

- CordaService is started during initiation of any Node. In this example, BlowWhistleStateObserver.kt is compiled and ran. 
- Any methods that you would like to run during the node initiation, you can put it inside the ```init``` block.  In this example, we are setting up an Vault update listener [line#30](https://github.com/corda/samples/blob/anonymous-whistleblowing/whistleblower/src/main/kotlin/com/whistleblower/BlowWhistleStateObserver.kt#L30), which will initiate an AutoDirectFlow when there is a new whistle-blowing message sent to the SurveyMonkey.

