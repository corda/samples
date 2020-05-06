package net.corda.examples.bikemarket.states;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import net.corda.examples.bikemarket.contracts.FrameContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
@BelongsToContract(FrameContract.class)
public class FrameTokenState extends EvolvableTokenType {


    private final Party maintainer;
    private final UniqueIdentifier uniqueIdentifier;
    private final int fractionDigits;
    private final String ModelNum;

    public FrameTokenState(Party maintainer, UniqueIdentifier uniqueIdentifier, int fractionDigits, String ModelNum) {
        this.maintainer = maintainer;
        this.uniqueIdentifier = uniqueIdentifier;
        this.fractionDigits = fractionDigits;
        this.ModelNum = ModelNum;
    }

    public String getModelNum() {
        return ModelNum;
    }


    public Party getIssuer() {
        return maintainer;
    }

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return Arrays.asList(maintainer);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.uniqueIdentifier;
    }
}
