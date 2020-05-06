# autoPayroll -- CordaService Demo
<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

## Introduction 
This Cordapp shows how to trigger a flow with vault update(completion of prior flows) using `CordaService` & `trackby`.

In this Cordapp, there are four parties: 
 - Finance Team: gives payroll order
 - Bank Operater: take the order and automatically initiate the money transfer
 - PetersonThomas: worker #1 will accept money
 - GeorgeJefferson: worker #2 will accept money
 
There are two states `PaymentRequestState` & `MoneyState`, and two flows `RequestFlow` & `PaymentFlow`. The business logic looks like the following: 
![alt text](https://github.com/corda/samples/blob/add-samples/autopayroll-CordaService/webpic/Business%20Logic.png)

1. Finance team put in payroll request to the bank operators
2. Bank operator receives the requests and process them without stopping 

## Running the demo 
Deploy and run the nodes by:

- Java use the `workflows-java:deployNodes` task and `./workflows-java/build/nodes/runnodes` script.

- Kotlin use the `workflows-kotlin:deployNodes` task and `./workflows-kotlin/build/nodes/runnodes` script.

if you have any questions during setup, please go to https://docs.corda.net/getting-set-up.html for detailed setup instructions. 

Once all four nodes are started up, in Financeteam's node shell, run: 
```
flow start RequestFlowInitiator amount: 500, towhom: GeorgeJefferson
```
As a result, we can check for the payment at GeorgeJefferson's node shell by running: 
```
run vaultQuery contractStateType: net.corda.examples.autopayroll.states.MoneyState
```
We will see that George Jefferson received a `MoneyState` with amount $500.

Behind the scenes, upon the completion of `RequestFlow`, a request state is stored at Bank operator's vault. The CordaService vault listener picks up the update and calls the `paymentFlow` automatically to send a `moneyState` to the designed reciever.

## Flow triggering using CordaService
The CordaService that triggers the flow is defined in AutoPaymentService.kt. The `CordaService` annotation is used by Corda to find any services that should be created on startup. In order for a flow to be startable by a service, the flow must be annotated with @StartableByService. An example is given in PaymentFlow.kt.
You probably have noticed that `paymentFlow` is not tagged with `@StartableByRPC` like flows normally are. That is, it will not show up in the node shell's flow list. The reason is that `paymentflow` is a completely automated process that does not need any external interactions, so it is ok to be "not-been-seen" from the RPC.

That said, CordaService broadly opens up the probabilities of writing automated flows and fast responding Cordapps! 
