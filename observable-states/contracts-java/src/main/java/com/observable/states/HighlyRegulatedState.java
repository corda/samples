package com.observable.states;

import com.observable.contracts.HighlyRegulatedContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(HighlyRegulatedContract.class)
public class HighlyRegulatedState implements ContractState {

    private final Party buyer;
    private final Party seller;

    public HighlyRegulatedState(Party buyer, Party seller) {
        this.buyer = buyer;
        this.seller = seller;
    }

    public Party getBuyer() {
        return buyer;
    }

    public Party getSeller() {
        return seller;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(buyer, seller);
    }
}
