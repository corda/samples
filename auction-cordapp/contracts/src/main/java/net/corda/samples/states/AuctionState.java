package net.corda.samples.states;

import net.corda.core.contracts.*;
import net.corda.core.flows.FlowLogicRef;
import net.corda.core.flows.FlowLogicRefFactory;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.contracts.AuctionContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * A ContractState to represent an auction on Corda Ledger. This is created as a SchedulableState to allow the
 * scheduling of ending the auction at a predefined time, which is set while creating the auction.
 */
@BelongsToContract(AuctionContract.class)
public class AuctionState implements SchedulableState {

    private final LinearPointer<LinearState> auctionItem;
    private final UUID auctionId;
    private final Amount<Currency> basePrice;
    private final Amount<Currency> highestBid;
    private final Party highestBidder;
    private final Instant bidEndTime;
    private final Amount<Currency> winningBid;
    private final Boolean active;

    private final Party auctioneer;
    private final List<Party> bidders;
    private final Party winner;

    /**
     *
     * @param auctionItem is a pointer to the item in auction
     * @param auctionId is a unique id of the auction
     * @param basePrice of the item on auction
     * @param highestBid made on the auction at any point in time
     * @param highestBidder is the party who made the highest bid at any point in time
     * @param bidEndTime is the deadline for the auction
     * @param winningBid is the highest bid made till deadline
     * @param active defines if the auction is still accepting bids/ deadline has passed
     * @param auctioneer is the party who started the auction
     * @param bidders are all the parties who can bid on the auction
     * @param winner is the party who made the highest bid and won the bidding
     */
    public AuctionState(LinearPointer<LinearState> auctionItem, UUID auctionId, Amount<Currency> basePrice, Amount<Currency> highestBid,
                        Party highestBidder, Instant bidEndTime, Amount<Currency> winningBid, Boolean active, Party auctioneer,
                        List<Party> bidders, Party winner) {
        this.auctionItem = auctionItem;
        this.auctionId = auctionId;
        this.basePrice = basePrice;
        this.highestBid = highestBid;
        this.highestBidder = highestBidder;
        this.bidEndTime = bidEndTime;
        this.winningBid = winningBid;
        this.active = active;
        this.auctioneer = auctioneer;
        this.bidders = bidders;
        this.winner = winner;
    }

    /**
     * This method returns a ScheduledActivity. The ScheduledActivity encapsulates a flow ref and a trigger time
     * to start the ScheduledFlow.
     *
     * @param thisStateRef
     * @param flowLogicRefFactory
     * @return a ScheduledActivity to be triggered at a specific instant.
     */
    @Nullable
    @Override
    public ScheduledActivity nextScheduledActivity(@NotNull StateRef thisStateRef,
                                                   @NotNull FlowLogicRefFactory flowLogicRefFactory) {
        if(!active)
            return null;

        FlowLogicRef flowLogicRef = flowLogicRefFactory.create(
                "net.corda.samples.flows.EndAuctionFlow$Initiator", auctionId);
        return new ScheduledActivity(flowLogicRef, bidEndTime);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> allParties = new ArrayList<>(bidders);
        allParties.add(auctioneer);
        return allParties;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public Amount<Currency> getBasePrice() {
        return basePrice;
    }

    public LinearPointer<LinearState> getAuctionItem() {
        return auctionItem;
    }

    public Amount<Currency> getHighestBid() {
        return highestBid;
    }

    public Party getHighestBidder() {
        return highestBidder;
    }

    public Amount<Currency> getWinningBid() {
        return winningBid;
    }

    public Instant getBidEndTime() {
        return bidEndTime;
    }


    public Party getAuctioneer() {
        return auctioneer;
    }

    public List<Party> getBidders() {
        return bidders;
    }

    public Party getWinner() {
        return winner;
    }

    public Boolean getActive() {
        return active;
    }
}
