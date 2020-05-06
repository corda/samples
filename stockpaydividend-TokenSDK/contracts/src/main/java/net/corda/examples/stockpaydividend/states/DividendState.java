package net.corda.examples.stockpaydividend.states;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.examples.stockpaydividend.contracts.DividendContract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

@BelongsToContract(DividendContract.class)
public class DividendState implements LinearState {

    private final UniqueIdentifier linearId;
    private Party company;
    private Party shareholder;
    private Date payDate;
    private Amount<TokenType> dividendAmount;
    private Boolean paid;

    public DividendState(UniqueIdentifier linearId, Party company, Party shareholder, Date payDate, Amount<TokenType> dividendAmount, Boolean paid) {
        this.linearId = linearId;
        this.company = company;
        this.shareholder = shareholder;
        this.payDate = payDate;
        this.dividendAmount = dividendAmount;
        this.paid = paid;
    }

    @NotNull
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    public Party getIssuer() { return company; }
    public Party getHolder() { return shareholder; }
    public Date getPayDate() { return payDate; }
    public Amount<TokenType> getDividendAmount() { return dividendAmount; }
    public Boolean getPaid() {
        return paid;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(company, shareholder);
    }


}
