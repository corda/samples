package net.corda.examples.attachments.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import net.corda.examples.attachments.contracts.AgreementContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@CordaSerializable
@BelongsToContract(AgreementContract.class)
public class AgreementState implements ContractState {
    private final Party partyA;
    private final Party partyB;
    private final String txt;

    public Party getPartyA() {
        return partyA;
    }
    public Party getPartyB() { return partyB; }
    public String getTxt() {
        return txt;
    }

    @ConstructorForDeserialization
    public AgreementState(Party partyA, Party partyB, String txt) {
        this.partyA = partyA;
        this.partyB = partyB;
        this.txt = txt;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(partyA, partyB);
    }
}
