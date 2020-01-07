package com.example.state;

import com.example.contract.SanctionedEntitiesContract;
import com.google.common.collect.ImmutableList;
import jdk.nashorn.internal.ir.annotations.Immutable;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;

import java.util.List;

/**
 * The state object recording list of untrusted parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 */

@BelongsToContract(SanctionedEntitiesContract.class)
public class SanctionedEntities implements LinearState {
    private List<Party> badPeople;
    private Party issuer;
    private UniqueIdentifier linearID;

    public List<Party> getBadPeople() {
        return badPeople;
    }

    public Party getIssuer() {
        return issuer;
    }

    /**
     * @param badPeople list of untrusted parties.
     * @param issuer the party issuing the sanctioned list
     */
    @ConstructorForDeserialization
    public SanctionedEntities(List<Party> badPeople, Party issuer, UniqueIdentifier linearID) {
        this.badPeople = badPeople;
        this.issuer = issuer;
        this.linearID = linearID;
    }

    public SanctionedEntities(List<Party> badPeople, Party issuer) {
        this(badPeople, issuer, new UniqueIdentifier());
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearID;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(issuer);
    }
}
