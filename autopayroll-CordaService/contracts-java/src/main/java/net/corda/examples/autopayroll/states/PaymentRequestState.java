package net.corda.examples.autopayroll.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.examples.autopayroll.contracts.PaymentRequestContract;

import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(PaymentRequestContract.class)
public class PaymentRequestState implements ContractState {
    private final String amount;
    private final Party towhom;
    private final List<AbstractParty> participants;

    public PaymentRequestState(String amount, Party towhom, List<AbstractParty> participants) {
        this.amount = amount;
        this.towhom = towhom;
        this.participants = participants;
    }

    public String getAmount() {
        return amount;
    }

    public Party getTowhom() {
        return towhom;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}
