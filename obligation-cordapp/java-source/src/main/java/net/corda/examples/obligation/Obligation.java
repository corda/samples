package net.corda.examples.obligation;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.corda.core.utilities.EncodingUtils.toBase58String;

@BelongsToContract(ObligationContract.class)
public class Obligation implements LinearState {
    private final Amount<Currency> amount;
    private final AbstractParty lender;
    private final AbstractParty borrower;
    private final Amount<Currency> paid;
    private final UniqueIdentifier linearId;

    @ConstructorForDeserialization
    public Obligation(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower, Amount<Currency> paid, UniqueIdentifier linearId) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    public Obligation(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower, Amount<Currency> paid) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = new UniqueIdentifier();
    }

    public Obligation(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = new Amount<>(0, amount.getToken());
        this.linearId = new UniqueIdentifier();
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public AbstractParty getLender() {
        return lender;
    }

    public AbstractParty getBorrower() {
        return borrower;
    }

    public Amount<Currency> getPaid() {
        return paid;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    public Obligation pay(Amount<Currency> amountToPay) {
        return new Obligation(
                this.amount,
                this.lender,
                this.borrower,
                this.paid.plus(amountToPay),
                this.linearId
        );
    }

    public Obligation withNewLender(AbstractParty newLender) {
        return new Obligation(this.amount, newLender, this.borrower, this.paid, this.linearId);
    }

    public Obligation withoutLender() {
        return new Obligation(this.amount, NullKeys.INSTANCE.getNULL_PARTY(), this.borrower, this.paid, this.linearId);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String lenderString;
        if (this.lender instanceof Party) {
            lenderString = ((Party) lender).getName().getOrganisation();
        } else {
            PublicKey lenderKey = this.lender.getOwningKey();
            lenderString = toBase58String(lenderKey);
        }

        String borrowerString;
        if (this.borrower instanceof Party) {
            borrowerString = ((Party) borrower).getName().getOrganisation();
        } else {
            PublicKey borrowerKey = this.borrower.getOwningKey();
            borrowerString = toBase58String(borrowerKey);
        }

        return String.format("Obligation(%s): %s owes %s %s and has paid %s so far.",
                this.linearId, borrowerString, lenderString, this.amount, this.paid);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Obligation)) {
            return false;
        }
        Obligation other = (Obligation) obj;
        return amount.equals(other.getAmount())
                && lender.equals(other.getLender())
                && borrower.equals(other.getBorrower())
                && paid.equals(other.getPaid())
                && linearId.equals(other.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, lender, borrower, paid, linearId);
    }
}