package com.example.state;

import com.example.contract.SanctionableIOUContract;
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
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 */
@BelongsToContract(SanctionableIOUContract.class)
public class SanctionableIOUState implements LinearState {
    private int value;
    private Party lender;
    private Party borrower;
    private UniqueIdentifier linearID;

     /**
      * @param value the value of the IOU.
      * @param lender the party issuing the IOU.
      * @param borrower the party receiving and approving the IOU.
    */
    @ConstructorForDeserialization
    public SanctionableIOUState(int value, Party lender, Party borrower, UniqueIdentifier linearID) {
        this.value = value;
        this.lender = lender;
        this.borrower = borrower;
        this.linearID = linearID;
    }

    public int getValue() {
        return value;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public SanctionableIOUState(int value, Party lender, Party borrower) {
        this(value, lender, borrower, new UniqueIdentifier());
    }

    @Override
    /** The public keys of the involved parties. */
    public UniqueIdentifier getLinearId() {
        return this.linearID;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }
}
