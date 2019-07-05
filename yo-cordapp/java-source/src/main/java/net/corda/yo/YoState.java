package net.corda.yo;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class YoState implements ContractState {
    private Party origin;
    private Party target;
    private String yo;

    public Party getOrigin() {
        return origin;
    }

    public Party getTarget() {
        return target;
    }

    public String getYo() {
        return yo;
    }

    YoState(Party origin, Party target, String yo){
        this.origin = origin;
        this.target = target;
        this.yo = yo;
    }

    YoState(Party origin, Party target){
        this(origin, target, "Yo");
    }

    @Override
    public String toString(){
        return origin.nameOrNull() + "yo";
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return null;
    }
}


/*


// State.
@BelongsToContract(YoContract::class)
data class YoState(val origin: Party,
                 val target: Party,
                 val yo: String = "Yo!") : ContractState {
    override val participants = listOf(target)
    override fun toString() = "${origin.name}: $yo"
}
 */
