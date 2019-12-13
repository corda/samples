package net.corda.yo.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import net.corda.yo.contract.YoContract;

import java.util.List;

@CordaSerializable
@BelongsToContract(YoContract.class)
public class YoState implements ContractState {
    public AbstractParty origin;
    public AbstractParty target;
    public String yo;

    public AbstractParty getOrigin() {
        return origin;
    }

    public AbstractParty getTarget() {
        return target;
    }

    public String getYo() { return yo; }

    @ConstructorForDeserialization
    public YoState(AbstractParty origin, AbstractParty target, String yo){
        this.origin = origin;
        this.target = target;
        this.yo = yo;
    }

    public YoState(AbstractParty origin, AbstractParty target){
        this(origin,target,"yo");
    }

    @Override
    public String toString(){
        return origin.nameOrNull() + "yo";
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return  ImmutableList.of(target);
    }

}



