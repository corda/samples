<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Whistle Blower CorDapp

This CorDapp is a simple showcase of confidential identities (i.e. anonymous public keys).

A node (the *whistle-blower*) can whistle-blow on a company to another node (the *investigator*). Both the
whistle-blower and the investigator generate anonymous public keys for this transaction, meaning that any third-parties
who manage to get ahold of the state cannot identity the whistle-blower or investigator. This process is handled
automatically by the `SwapIdentitiesFlow`.

# Pre-requisites:

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

Java
``./gradlew workflows-java:deployNodes`` then ``./workflows-java/build/nodes/runnodes``

Kotlin
``./gradlew workflows-kotlin:deployNodes`` then ``./workflows-kotlin/build/nodes/runnodes``

## Interacting with the nodes:

We will interact with this CorDapp via the nodes' CRaSH shells.

First, go the the shell of BraveEmployee, and report BadCompany to the TradeBody by running:

    flow start BlowWhistleFlow badCompany: BadCompany, investigator: TradeBody

To see the whistle-blowing case stored on the whistle-blowing node, run:

    run vaultQuery contractStateType: net.corda.examples.whistleblower.states.BlowWhistleState

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

We can also see the whistle-blowing case stored on the investigator node.

