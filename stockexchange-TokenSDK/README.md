<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# TokenSDK Sample - Stock Exchange CorDapp - Java
This CorDapp aims to demonstrate the usage of TokenSDK, especially the concept of EvolvableToken which represents stock.
You will find the StockState extends from EvolvableToken which allows the stock information to be updated without affecting the parties who own the stock.

This Stock Exchange CorDapp includes:
* A company issues and moves stocks to shareholders
* Stock issuer announces dividends for shareholders to claim before execution day
* Shareholder retrieves the most updated stock information and then claims dividend
* Stock issuer distribute dividends to shareholders

### Running the nodes:
See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Sample Overview
![Overview flow diagram](diagrams/FlowDiagram.png)

### Keys to learn
* Basic usage of TokenSDK
* How the state of stock (ie. EvolvableToken) updates independently without stock holders involved 
* Use of `TokenSelection.generateMove()` and `MoveTokensUtilitiesKt.addMoveTokens()` to generate move of tokens
* Adding observers in token transactions with TokenSDK 

*Note that some date constraint(eg. payday) is being commented out to make sure the sample can be ran smoothly  

### Roles
This CordApp assumes there are 4 parties
* **Issuer** - who creates and maintains the stock state and pay dividends to shareholders after time.
* **Holder** - who shareholder receives dividends base on the owning stock.
* **Bank** - who issues fiat tokens.
* **Observer** - who keeps a copy whenever a stock is created or updated. 
<br>In real life, it should be the financial regulatory authorities like SEC  

### Running the sample
To go through the sample flow, execute the commands on the corresponding node  

##### 1. IssueMoney - Issuer
Bank issues some fiat currencies to the issuer for paying off dividends later. 
>On issuer node, <br>execute `start IssueMoney currency: USD, amount: 500, recipient: Issuer`

##### 2. IssueStock - Issuer
Issuer creates a StockState and issues some stock tokens associated to the created StockState.
>On issuer node, <br>execute `start IssueStock symbol: TEST, name: "Stock, SP500", currency: USD, issueVol: 500, notary: Notary`

##### 3. MoveStock - Issuer
Issuer transfers some stock tokens to the Holder.
>On issuer node, <br>execute `start MoveStock symbol: TEST, quantity: 100, recipient: Holder`

Now at the Shareholder's terminal, we can see that it recieved 100 stock tokens:
>On Shareholder node, <br>execute `run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken`

##### 4. AnnounceDividend - Issuer
Issuer announces the dividends that will be paid on the payday.
>On issuer node, <br>execute `start AnnounceDividend symbol: TEST, dividendQuantity: 0.05, executionDate: "2019-11-22T00:00:00Z", payDate: "2019-11-23T00:00:00Z"`

##### 5. GetStockUpdate - Holder
Shareholders retrieves the newest stock state from the issuer. 
>On holder node, <br>execute `start GetStockUpdate symbol: TEST`

##### 6. ClaimDividendReceivable - Holder
Shareholders finds the dividend is announced and claims the dividends base on the owning stock. 
>On holder node, <br>execute `start ClaimDividendReceivable symbol: TEST`

##### 7. PayDividend - Issuer
On the payday, the issuer pay off the stock with fiat currencies.
>On issuer node, <br>execute `start PayDividend`

##### Query. Get token balances
> Get stock token balances 
<br>`start GetStockBalance symbol: TEST`

>Get fiat token balances
<br>`start GetFiatBalance currencyCode: USD`

#### Test case
You can also find the flow and example data from the test class [FlowTests.java](workflows/src/test/java/net/corda/examples/stockexchange/FlowTests.java).
 
### Useful links
##### Documentations
[Token-SDK tutorial](https://github.com/corda/token-sdk/blob/master/docs/DvPTutorial.md)
<br>
[Token-SDK design document](https://github.com/corda/token-sdk/blob/95b7bac668c68f3108bca2c50f4f926d147ee763/design/design.md#evolvabletokentype)

##### Other materials
[Blog - House trading sample](https://medium.com/corda/lets-create-some-tokens-5e7f94c39d13) - 
A less complicated sample of TokenSDK about trading house.
<br>
[Blog - Introduction to Token SDK in Corda](https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025) -
Provides basic understanding from the ground up.
<br>
[Sample - TokenSDK with Account](https://github.com/corda/accounts/tree/master/examples/tokens-integration-test)
An basic sample of how account feature can be integrated with TokenSDK

