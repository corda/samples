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

## Interacting with the nodes:

You interact with this CorDapp using its web API. Each node exposes this web API on a different address:

* BraveEmployee: `localhost:10012/`
* TradeBody `localhost:10016/`
* BadCompany: `localhost:10020/`

The web API for each node exposes two endpoints:

* `/api/a/cases`, which lists the `BlowWhistleState`s in which the node is either the whistle-blower or
  investigator
* `/api/a/blow-whistle?company=X&to=Y`, which causes the node to report company X to investigator Y
  
For example, BraveEmployee can report BadCompany to the TradeBody by visiting the following URL:

    http://localhost:10012/api/a/blow-whistle?company=BadCompany&to=TradeBody

You should see the following message:

    C=KE,L=Nairobi,O=BraveEmployee reported BadCompany to TradeBody.
    
If you now visit `http://localhost:10012/api/a/cases`, you should see the whistle-blowing case stored on the
whistle-blowing node:

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

As we can see, the whistle-blower and investigator are identified solely by anonymous public keys. If we whistle-blow 
again:

    http://localhost:10012/api/a/blow-whistle?company=BadCompany&to=TradeBody

Then when we look at the list of cases:
    
    `http://localhost:10012/api/a/cases`
    
We'll see that even though in both cases the same whistle-blower and investigator were involved, the public keys used 
to identify them are completely different, preserving their anonymity.
