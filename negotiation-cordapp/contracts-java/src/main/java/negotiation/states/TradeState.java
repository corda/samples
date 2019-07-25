package negotiation.states;

import com.google.common.collect.ImmutableList;
import negotiation.contracts.ProposalAndTradeContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;

import java.util.List;

@BelongsToContract(ProposalAndTradeContract.class)
public class TradeState implements LinearState {
    private int amount;
    private Party buyer;
    private Party seller;
    private UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public TradeState(int amount, Party buyer, Party seller, UniqueIdentifier linearId) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.linearId = linearId;
    }

    public TradeState(int amount, Party buyer, Party seller) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.linearId = new UniqueIdentifier();
    }

    public int getAmount() {
        return amount;
    }

    public Party getBuyer() {
        return buyer;
    }

    public Party getSeller() {
        return seller;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(buyer,seller);
    }
}
