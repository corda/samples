package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This flow is used to create a auction for an asset.
 */
public class CreateAuctionFlow {

    private CreateAuctionFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final ProgressTracker progressTracker = new ProgressTracker();

        private final Amount<Currency> basePrice;
        private final UUID auctionItem;
        private final LocalDateTime bidDeadLine;

        /**
         * Constructor to initialise flow parameters received from rpc.
         *
         * @param basePrice of the asset to be put of auction.
         * @param auctionItem is the uuid of the asset to be put on auction
         * @param bidDeadLine is the time till when the auction will be active
         */
        public Initiator(Amount<Currency> basePrice, UUID auctionItem, LocalDateTime bidDeadLine) {
            this.basePrice = basePrice;
            this.auctionItem = auctionItem;
            this.bidDeadLine = bidDeadLine;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Fetch the notary for the transaction
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party auctioneer = getOurIdentity();

            // Fetch all parties from the network map and remove the auctioneer and notary. All the parties are added as
            // participants to the auction state so that its visible to all the parties in the network.
            List<Party> bidders = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                    .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                    .collect(Collectors.toList());
            bidders.remove(auctioneer);
            bidders.remove(notary);

            // Create the output state. Use a linear pointer to point to the asset on auction. The asset would be added
            // as a reference state to the transaction and hence we won't spend it.
            AuctionState auctionState = new AuctionState(
                    new LinearPointer<>(new UniqueIdentifier(null, auctionItem), LinearState.class),
                    UUID.randomUUID(), basePrice, null, null,
                    bidDeadLine.atZone(ZoneId.systemDefault()).toInstant(), null, true, auctioneer,
                    bidders, null);

            // Build the transaction
            TransactionBuilder builder = new TransactionBuilder(notary)
                .addOutputState(auctionState)
                .addCommand(new AuctionContract.Commands.CreateAuction(), auctioneer.getOwningKey());

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow to notarise the transaction and record it in all participants ledger.
            List<FlowSession> bidderSessions = new ArrayList<>();
            for(Party bidder: bidders)
                bidderSessions.add(initiateFlow(bidder));

            return subFlow(new FinalityFlow(selfSignedTransaction, bidderSessions));
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
