package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AssetContract;
import net.corda.samples.states.Asset;

import java.util.Collections;


/**
 * This flow is used to build a transaction to issue an asset on the Corda Ledger, which can later be put on auction.
 * It creates a self issues transaction, the state is only issued on the ledger of the party who executes the flow.
 */
@InitiatingFlow
@StartableByRPC
public class CreateAssetFlow extends FlowLogic<SignedTransaction> {

    private final String title;
    private final String description;
    private final String imageURL;

    /**
     * Constructor to initialise flow parameters received from rpc.
     *
     * @param title of the asset to be issued on ledger
     * @param description of the asset to be issued in ledger
     * @param imageURL is a url of an image of the asset
     */
    public CreateAssetFlow(String title, String description, String imageURL) {
        this.title = title;
        this.description = description;
        this.imageURL = imageURL;
    }


    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // Choose a notary for the transaction.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Create the output state
        Asset output = new Asset(new UniqueIdentifier(), title, description, imageURL,
                getOurIdentity());

        // Build the transaction, add the output state and the command to the transaction.
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(output)
                .addCommand(new AssetContract.Commands.CreateAsset(),
                        getOurIdentity().getOwningKey()); // Required Signers

        // Verify the transaction
        transactionBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        // Notarise the transaction and record the state in the ledger.
        return subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
    }
}