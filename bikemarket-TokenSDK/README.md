# Bike Market - TokenSDK

<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

## Introduction 
This sample Cordapp demonstrate some simple flows related to the token SDK. In this Cordapp,there are four parties: 
- The Bike Company (BikeCo): can manufacture, sell, and recall/total the bikes(or parts). 
- The Licensed Dealership: Buy the bikes from the BikeCo
- Used Parts Agency: Buy used parts from the Licensed Dealership(or end-buyers)
- Buyer: Buy bike from the BikeCo or licensed dealership, or buy used parts from used parts agency. 

In this sample Cordapp, we will mimic a bike buying and selling market. 

![alt text](https://github.com/corda/samples/blob/dvp-token/bikemarket-TokenSDK/diagram/pic1.png)

From the above chat we see that Tokens are representing the ownership and status of the physical assests, such as bike frame and bike wheels. A key point to notice here is that **a bike is represented with 2 tokens (Frame and wheels)**. This is designed in the way to be flexiable to sell or total a specific part of your bike. As can see, this bike buying/selling market is capable of mimicing multiple business logics. We will be demonstrating one of the possible logic here:
1. BikeCo manufactures the bikes
2. BikeCo can sell the bike to licened dealership and buyers. 
3. Used parts agency can get the used bike parts from the licened dealership or buyers. 
4. When there is a need of total the physical bike part, the current of the physical part will redeem the token with the BikeCo

Through out the sample, we will see how to create, transacte, and redeem a token. 

## Running the sample
Deploy and run the nodes by:
```
./gradlew deployNodes
./build/nodes/runnodes
```
if you have any questions during setup, please go to https://docs.corda.net/getting-set-up.html for detailed setup instructions.

Once all four nodes are started up, in BikeCo's node shell, run: 
```
flow start CreateFrameToken frameSerial: F4561
flow start CreateWheelToken wheelSerial: W7894 
```
After this step, we have created 2 tokens representing the physical bike part with unique serial number(which will be unique in the manufacturing). 
Then run:
```
flow start IssueNewBike frameSerial: F4561, wheelSerial: W7894, holder: LicensedDealership
```
This line of command will transfer the tokens(2 tokens together represents a single bike) to the licensed dealership. 

Now, at the licensed dealership's shell, we can see we did recieve the tokens by running: 
```
run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
```
Continue to the business flow, the licensed dealership will sell the bike to the Buyer. Run: 
```
flow start TransferBikeToken frameSerial: F4561, wheelSerial: W7894, holder: Buyer
```

Now we can check at the Buyer's node shell to see if the buyer recieves the token by running the same `vaultQuery` we just ran at the dealership's shell. 

At the Buyer side, we would assume we got a recall notice and will send the physical bike frame back to the manufacturer. The action will happen in real life, but on the ledger we will also need to "destroy"(process of redeem in Corda TokenSDK) the frame token. Run:
```
flow start TotalPart part: frame, serialNumber: F4561
```
At the buyer's shell, if we do the `vaultQuery` again, we will see we now only have a wheel token(the frame token is gone). With the wheel token, we can sell this pair of wheels to the used parts agency. We will achieve it by running: 
```
flow start TransferPartToken part: wheel, serialNumber: W7894, holder: UsedPartsAgency
```
At the end of the flow logic, we will find the frame token is destroyed and the used parts agency holds the wheel token. 





