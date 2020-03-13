package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is a scheduled flow, scheduled to run when the auction deadline is reached. It marks the auction as inactive so
 * that no new bids are accepted.
 */
public class EndAuctionFlow {

    private EndAuctionFlow() {}

    //Scheduled Flows must be annotated with @SchedulableFlow.
    @SchedulableFlow
    @InitiatingFlow
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private final UUID auctionId;

        /**
         * @param auctionId is the unique identifier of the auction to be closed.
         */
        public Initiator(UUID auctionId) {
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
            // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
            // transaction.
            List<StateAndRef<AuctionState>> auctionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();
            StateAndRef<AuctionState> inputStateAndRef = auctionStateAndRefs.stream().filter(auctionStateAndRef -> {
                AuctionState auctionState = auctionStateAndRef.getState().getData();
                return auctionState.getAuctionId().toString().equals(this.auctionId.toString());
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));

            //get the notary from the input state.
            Party notary = inputStateAndRef.getState().getNotary();
            AuctionState inputState = inputStateAndRef.getState().getData();

            // Check used to restrict the flow execution to be only done by the auctioneer.
            if (getOurIdentity().getName().toString().equals(inputState.getAuctioneer().getName().toString())) {
                // Create the output state, mark tge auction as inactive
                AuctionState outputState = new AuctionState(inputState.getAuctionItem(), inputState.getAuctionId(),
                        inputState.getBasePrice(), inputState.getHighestBid(), inputState.getHighestBidder(),
                        inputState.getBidEndTime(), inputState.getHighestBid(), false, inputState.getAuctioneer(),
                        inputState.getBidders(), inputState.getHighestBidder());

                // Build the transaction.
                TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                        .addInputState(inputStateAndRef)
                        .addOutputState(outputState)
                        .addCommand(new AuctionContract.Commands.EndAuction(), getOurIdentity().getOwningKey());

                //Verify the transaction against the contract
                transactionBuilder.verify(getServiceHub());

                //Sign the transaction.
                SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

                //Notarize and record the transaction in all participants ledger.
                List<FlowSession> bidderSessions = new ArrayList<>();
                for (Party bidder : inputState.getBidders())
                    bidderSessions.add(initiateFlow(bidder));
                return subFlow(new FinalityFlow(signedTransaction, bidderSessions));
            } else {
                return null;
            }
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