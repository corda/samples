<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Yo! CorDapp

Send Yo's! to all your friends running Corda nodes!

## Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

## Usage

### Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

### Sending Yo!s

We will interact with the nodes via their shell. When the nodes are up and running, use the following command to send a
Yo! to another node:

    flow start YoFlow target: PartyB

Where `NODE_NAME` is 'PartyA' or 'PartyB'. The space after the `:` is required. You are not required to use the full
X500 name in the node shell. Note you can't sent a Yo! to yourself because that's not cool!

To see all the Yo's! other nodes have sent you in your vault (you do not store the Yo's! you send yourself), run:

    run vaultQuery contractStateType: com.yo.states.YoState
