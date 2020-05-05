package com.template.states;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.template.contracts.IplTicketContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(IplTicketContract.class)
public class IplTicket extends EvolvableTokenType {

    private UniqueIdentifier linearId;
    private String ticketTeam;
    private Party issuer;//dealer
    //private AbstractParty owner;//for the first time dealer then customers

    public IplTicket(UniqueIdentifier linearId, String ticketTeam, Party issuer) {
        this.linearId = linearId;
        this.ticketTeam = ticketTeam;
        this.issuer = issuer;
        //this.owner = owner;
    }

    @Override
    public int getFractionDigits() {
        return 0;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return Arrays.asList(this.getIssuer());
    }

    //TODO try removing issuer from participant, make him an observer, see how updatedistribution works and ObserverAwareFinalityFlow.
//    @NotNull
//    @Override
//    public List<AbstractParty> getParticipants() {
//        return Arrays.asList(this.getOwner());
//    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }

//    public AbstractParty getOwner() {
//        return owner;
//    }

    public String getTicketTeam() {
        return ticketTeam;
    }

    public Party getIssuer() {
        return issuer;
    }

}
