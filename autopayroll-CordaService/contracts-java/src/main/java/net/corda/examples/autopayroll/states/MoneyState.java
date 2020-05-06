package net.corda.examples.autopayroll.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.examples.autopayroll.contracts.MoneyStateContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(MoneyStateContract.class)
public class MoneyState implements ContractState {

    private final int amount;
    private final Party receiver;

    public MoneyState(int amount, Party receiver) {
        this.amount = amount;
        this.receiver = receiver;
    }

    public int getAmount() {
        return amount;
    }

    public Party getReceiver() {
        return receiver;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Collections.singletonList(receiver);
    }
}
