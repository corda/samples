<p align="center">
  <img src="https://www.corda.net/wp-content/themes/corda/assets/images/crda-logo-big.svg" alt="Corda" width="500">
  
</p>

# NodeInfo

Allows one to get some rudimentary information about a running Corda node via RPC

Useful for debugging network issues, ensuring flows have loaded etc.

After cloning, use the _getInfo_ gradle task to retrieve node information.

## How to install and run

The node you wish to retrieve information on must already be running. Then do:

    ./gradlew getInfo -Phost="[NODE_RPC_ADDRESS]" -Pusername="[RPC_USERNAME]" -Ppassword="[RPC_PASSWORD]"

For example, to run this utility against Party A defined in the CorDapp Example, you would run:

    ./gradlew getInfo -Phost="localhost:10006" -Pusername="user1" -Ppassword="test"

## Sample Output
  
```
./gradlew getInfo -Phost="localhost:10006" -Pusername="user1" -Ppassword="test"

> Task :getInfo
Logging into localhost:10006 as user1
Node connected: O=PartyA, L=London, C=GB
Time: 2018-02-27T11:30:37.729Z.
Flows: [com.example.flow.ExampleFlow$Initiator, net.corda.core.flows.ContractUpgradeFlow$Authorise, net.corda.core.flows.ContractUpgradeFlow$Deauthorise, net.corda.core.flows.ContractUpgradeFlow$Initiate, net.corda.finance.flows.CashConfigDataFlow, net.corda.finance.flows.CashExitFlow, net.corda.finance.flows.CashIssueAndPaymentFlow, net.corda.finance.flows.CashIssueFlow, net.corda.finance.flows.CashPaymentFlow]
Platform version: 2
Current Network Map Status -->
-- O=PartyA, L=London, C=GB @ localhost
-- O=Controller, L=London, C=GB @ localhost
-- O=PartyB, L=New York, C=US @ localhost
-- O=PartyC, L=Paris, C=FR @ localhost
-- O=Notary, L=London, C=GB @ localhost
Registered Notaries -->
-- O=Notary, L=London, C=GB
-- O=Controller, L=London, C=GB
```

## Errors

`Exception in thread "main" ActiveMQSecurityException[errorType=SECURITY_EXCEPTION message=AMQ119031: Unable to validate user]` 

Caused by: Wrong RPC credentials. Check the node.conf file and try again.

`Exception in thread "main" ActiveMQNotConnectedException[errorType=NOT_CONNECTED message=AMQ119007: Cannot connect to server(s). Tried with all available servers.]`

Caused by: Network connectivity issues or node not running. 
