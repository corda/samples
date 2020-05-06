package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * This flow is used to put a bid on an asset put on auction.
 */
public class BidFlow {

    private BidFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private final Amount<Currency> bidAmount;
        private final UUID auctionId;

        /**
         * Constructor to initialise flow parameters received from rpc.
         *
         * @param bidAmount is the amount the bidder is bidding for for the asset on auction.
         * @param auctionId is the unique identifier of the auction on which this bid it put.
         */
        public Initiator(Amount<Currency> bidAmount, UUID auctionId) {
            this.bidAmount = bidAmount;
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
            // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
            // transaction.
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();

            StateAndRef<AuctionState> inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
                AuctionState auctionState = auctionStateAndRef.getState().getData();
                return auctionState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));


            AuctionState input = inputStateAndRef.getState().getData();


            //Create the output state
            AuctionState output = new AuctionState(input.getAuctionItem(), input.getAuctionId(), input.getBasePrice(),
                    bidAmount, getOurIdentity(), input.getBidEndTime(), null, true,
                    input.getAuctioneer(), input.getBidders(), null);

            // Build the transaction. On successful completion of the transaction the current auction state is consumed
            // and a new auction state is create as an output containg tge bid details.
            TransactionBuilder builder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(new AuctionContract.Commands.Bid(), getOurIdentity().getOwningKey());

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow to notarise and commit the transaction in all the participants ledger.
            List<FlowSession> allSessions = new ArrayList<>();
            List<Party> bidders = new ArrayList<>(input.getBidders());
            bidders.remove(getOurIdentity());
            for(Party bidder: bidders)
                allSessions.add(initiateFlow(bidder));

            allSessions.add(initiateFlow(input.getAuctioneer()));
            return subFlow(new FinalityFlow(selfSignedTransaction, allSessions));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}