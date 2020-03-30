package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.samples.states.Asset;
import net.corda.samples.states.AuctionState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * This flow takes care of the delivery-vs-payment for settlement of the auction. The auctioned asset's ownership is
 * transferred from the auctioneer to the highest bidder and the bid amount is transferred from the highest bidder to
 * the auctioneer in a single atomic transaction.
 */
public class AuctionDvPFlow {

    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private final UUID auctionId;
        private final Amount<Currency> payment;

        /**
         * Constructor to initialise flow parameters.
         *
         * @param auctionId is the unique id of the auction to be settled
         * @param payment is the bid amount which is required to be transferred from the highest bidded to auctioneer to
         *              settle the auction.
         */
        public Initiator(UUID auctionId, Amount<Currency> payment) {
            this.auctionId = auctionId;
            this.payment = payment;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
            // to fetch the desired AuctionState state from the vault.
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();
            StateAndRef<AuctionState> auctionStateAndRef = auntionStateAndRefs.stream().filter(stateAndRef -> {
                AuctionState auctionState = stateAndRef.getState().getData();
                return auctionState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new FlowException("Auction Not Found"));
            AuctionState auctionState = auctionStateAndRef.getState().getData();

            // Create a QueryCriteria to query the Asset.
            // Resolve the linear pointer in previously filtered auctionState to fetch the assetState containing
            // the asset's unique id.
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null, Collections.singletonList(auctionStateAndRef.getState().getData().getAuctionItem()
                    .resolve(getServiceHub()).getState().getData().getLinearId().getId()),
                    null, Vault.StateStatus.UNCONSUMED);

            // Use the vaultQuery with the previously created queryCriteria to fetch th assetState to be used as input
            // in the transaction.
            StateAndRef<Asset> assetStateAndRef = getServiceHub().getVaultService().
                    queryBy(Asset.class, queryCriteria).getStates().get(0);

            // Use the withNewOwner() of the Ownable state get the command and the output state to be used in the
            // transaction from ownership transfer of the asset.
            CommandAndState commandAndState = assetStateAndRef.getState().getData()
                    .withNewOwner(auctionState.getWinner());

            // Create the transaction builder.
            TransactionBuilder transactionBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(0));

            // Generate Spend for the Cash. The CashUtils generateSpend method can be used to update the transaction
            // builder with the appropriate inputs and outputs corresponding to the cash spending. A new keypair is
            // generated to sign the transaction, so that the the change returned to the spender after the cash is spend
            // is untraceable.
            Pair<TransactionBuilder, List<PublicKey>> txAndKeysPair =
                    CashUtils.generateSpend(getServiceHub(), transactionBuilder, payment, getOurIdentityAndCert(),
                            auctionState.getAuctioneer(), Collections.emptySet());
            transactionBuilder = txAndKeysPair.getFirst();

            // Update the transaction builder with the input and output for the asset's ownership transfer.
            transactionBuilder.addInputState(assetStateAndRef)
                    .addOutputState(commandAndState.getOwnableState())
                    .addCommand(commandAndState.getCommand(),
                            Collections.singletonList(auctionState.getAuctioneer().getOwningKey()));

            // Verify the transaction
            transactionBuilder.verify(getServiceHub());

            // Sign the transaction. The transaction should be sigend with the new keyPair generated for Cash spending
            // and the node's key.
            List<PublicKey> keysToSign = txAndKeysPair.getSecond();
            keysToSign.add(getOurIdentity().getOwningKey());
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, keysToSign);

            // Collect counterparty signature.
            FlowSession auctioneerFlow = initiateFlow(auctionState.getAuctioneer());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(selfSignedTransaction,
                    Collections.singletonList(auctioneerFlow)));

            // Notarize the transaction and record tge update in participants ledger.
            return subFlow(new FinalityFlow(signedTransaction, Collections.singletonList(auctioneerFlow)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{

        private FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            subFlow(new SignTransactionFlow(otherPartySession) {

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Additional Checks here
                }
            });
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }
}
