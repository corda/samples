<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# The Upgrade CorDapp

This directory contains a number of versions of the same CorDapp (based heavily on the Obligation sample CorDapp). It is
designed to showcase ways in which a CorDapp might be upgraded such that the app remains backwards compatible between
versions.

Note that this is a work in progress.

# Current versions

The CorDapp versions are described below:

## Version 1
The initial version of the CorDapp. This is the Obligation CorDapp using the version of FinalityFlow that was present in
Corda 3.

## Version 2
This version of the CorDapp upgrades to use the new version of the FinalityFlow present in Corda 4. To do this such that
 the app remains compatible with Version 1, the following must be done:
 - The flows using the new FinalityFlow must be versioned
 - The flows must detect if the counterparty is using the old version, and revert to the old API accordingly
 - The app must continue to use targetPlatformVersion = 3. This is because upgrading to targetPlatformVersion = 4 results
   in the old FinalityFlow APIs becoming unusable.
   
The upgrade path to move this app to targetPlatformVersion = 4 is to upgrade the app to Version 2 on all nodes, and then
to move from this to an app using targetPlatformVersion = 4. (Currently not written.)

# The Obligation CorDapp

This CorDapp comprises a demo of an IOU-like agreement that can be issued, transfered and settled confidentially. The CorDapp includes:

* An obligation state definition that records an amount of any currency payable from one party to another. The obligation state
* A contract that facilitates the verification of issuance, transfer (from one lender to another) and settlement of obligations
* Three sets of flows for issuing, transferring and settling obligations. They work with both confidential and non-confidential obligations

The CorDapp allows you to issue, transfer (from old lender to new lender) and settle (with cash) obligations. It also 
comes with an API and website that allows you to do all of the aforementioned things.

# Instructions for setting up

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.
