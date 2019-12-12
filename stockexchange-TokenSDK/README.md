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

### Running the nodes:
See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Sample Overview
![Overview flow diagram](diagrams/FlowDiagram.png)

### Roles
This CordApp assumes there are 4 parties
* **Issuer** - who creates and maintains the stock state and pay dividends to shareholders after time.
* **Holder** - who shareholder receives dividends base on the owning stock.
* **Bank** - who issue fiat currency tokens.
* **Observer** - who keeps the update or copy whenever the stock changes. Mostly the financial regulatory authorities like SEC  

### Running the sample
To go through the sample flow, execute the commands on the corresponding node  

1. IssueMoney - Issuer

Bank issues some fiat currencies to the issuer for paying off dividends later. 
>On issuer node, execute `start IssueMoney currency: USD, amount: 500, recipient: Issuer`

2. IssueStock - Issuer

Issuer creates a StockState and issues some stock tokens associated to the created StockState.
>On issuer node, execute `start IssueStock symbol: TEST, name: "Stock, SP500", currency: USD, issueVol: 500, notary: Notary`

3. MoveStock - Issuer

Issuer transfers some stock tokens to the Holder.
>On issuer node, execute `start MoveStock symbol: TEST, quantity: 100, recipient: Holder`

4. AnnounceDividend - Issuer

Issuer announces the dividends that will be paid on the payday.
>On issuer node, execute `start AnnounceDividend symbol: TEST, quantity: 0.05, executionDate: "2019-11-22T00:00:00Z", payDate: "2019-11-23T00:00:00Z"`

5. GetStockUpdate - Holder

Shareholders retrieves the newest stock state from the issuer. 
>On holder node, execute `start GetStockUpdate symbol: TEST`

6. ClaimDividendReceivable - Holder

Shareholders finds the dividend is announced and claims the dividends base on the owning stock. 
>On holder node, execute `start ClaimDividendReceivable symbol: TEST`

7. PayDividend - Issuer

On the payday, the issuer pay off the stock with fiat currencies.
>On issuer node, execute `start PayDividend`

You can also find the flow and example data from the test class FlowTests.java.

*Note that all date constraint(eg. exDay and payDay) is being omitted for simplification. 
  
---
### Links
The detailed definition of EvolvableToken can be found here
https://github.com/corda/token-sdk/blob/95b7bac668c68f3108bca2c50f4f926d147ee763/design/design.md#evolvabletokentype
