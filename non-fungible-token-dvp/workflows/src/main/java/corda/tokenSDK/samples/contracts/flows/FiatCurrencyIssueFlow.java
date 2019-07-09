package corda.tokenSDK.samples.contracts.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Currency;

@StartableByRPC
public class FiatCurrencyIssueFlow extends FlowLogic<SignedTransaction> {

    private final String currency;
    private final Long amount;
    private final Party recipient;

    public FiatCurrencyIssueFlow(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.amount = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        FiatCurrency token = new FiatCurrency(Currency.getInstance(currency));
        return subFlow(new IssueTokens<>(new Amount<>(amount, token), getOurIdentity(), recipient));
    }

}
