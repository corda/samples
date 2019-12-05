<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# TokenSDK Sample - Stock Exchange CorDapp - Java
This CorDapp aims to demonstrate the usage of TokenSDK, especially the concept of EvolvableToken which represents stock.
You will find the StockState extends from EvolvableToken which allows the stock information to be updated without affecting the parties who own the stock.

This Stock Exchange CorDapp includes:
* Issuing and transferring transferring stock
* Announcing dividends by updating the stock state
* Distribute dividends to shareholders

## Running the nodes:
 
See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.


##Sample Overview
### Roles
This sample assumes there are 3 parties in the network.
* **Issuer** - who creates and maintains the stock state and pay dividends to shareholders after time.
* **Holder** - The shareholder receives dividends base on the owning stock.
* **Observer** - who keeps the update or copy whenever the stock changes

### Designed Sequence
1. IssueMoney - Issuer
2. IssueStock - Issuer
3. MoveStock - Issuer
4. AnnounceDividend - Issuer
5. GetStockUpdate - Holder
6. ClaimDividendReceivable - Holder
7. PayDividend - Issuer

You can also find the flow and example data from the test class FlowTests.java.

#####1. IssueMoney
Issuer issues some fiat currencies to itself for paying off dividends later. 
#####2. IssueStock
Issuer creates a StockState and issues some stock tokens associated to the created StockState.
#####3. MoveStock
Issuer transfers some stock tokens to the Holder.
#####4. AnnounceDividend
Issuer announces the dividends that will be paid on the payday.
#####5. GetStockUpdate
Shareholders retrieves the newest stock state from the issuer. 
The stock may have changed the price or dividends may have been announced.  
#####6. ClaimDividendReceivable
Shareholders finds the dividend is announced and claims the dividends base on the owning stock. 
#####7. PayDividend
On the payday, the issuer pay off the stock with fiat currencies.

### Links
The detailed definition of EvolvableToken can be found here
https://github.com/corda/token-sdk/blob/95b7bac668c68f3108bca2c50f4f926d147ee763/design/design.md#evolvabletokentype
