<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Observable States CorDapp

This CorDapp shows how Corda's observable states feature works. Observable states is the ability for nodes who are not 
participants in a transaction to still store them if the transactions are sent to them.

In this CorDapp, we assume that when a seller creates some kind of `HighlyRegulatedState`, they must notify the state 
and national regulators. There are two ways to use observable states:

* By piggy-backing on `FinalityFlow`
* By distributing the transaction manually

The two approaches are functionally identical. 

In this CorDapp, the seller runs the `TradeAndReport` flow to create a new `HighlyRegulatedState`. Then we can see that the seller will:

* Distribute the state to the buyer and the `state regulator` using `FinalityFlow`
* Distribute the state to the `national regulator` manually using the `ReportManually` flow 

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

Java use the `workflows-java:deployNodes` task and `./workflows-java/build/nodes/runnodes` script.

Kotlin use the `workflows-kotlin:deployNodes` task and `./workflows-kotlin/build/nodes/runnodes` script.

## Interacting with the nodes:

Go to the CRaSH shell of Seller, and create a new `HighlyRegulatedState`

    start TradeAndReport buyer: Buyer, stateRegulator: StateRegulator, nationalRegulator: NationalRegulator

The state will be automatically reported to StateRegulator and NationalRegulator, even though they are not 
participants. Check this by going to the shell of either node and running:

    run vaultQuery contractStateType: com.observable.states.HighlyRegulatedState

You will see the new `HighlyRegulatedState` in the vault of both nodes.
