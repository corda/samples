package bootcamp;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* Our state, defining a shared fact on the ledger.
 * See src/main/java/examples/ArtState.java for an example. */
@BelongsToContract(TokenContract.class)
public class TokenState implements ContractState {

    private final Party issuer;
    private final Party owner;
    private final int amount;
    private final List<AbstractParty> participants;

    public TokenState(Party issuer, Party owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.participants = new ArrayList<>();
        participants.add(issuer);
        participants.add(owner);
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    @NotNull
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}