package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

@InitiatingFlow
@StartableByRPC
public class IssueMoney extends FlowLogic<SignedTransaction> {

    private String currency;
    private Long quantity;
    private Party recipient;

    // Using NetworkmapCache.getNotaryIdentities().get(0) is not encouraged due to multi notary is introduced
    // private Party notary;

    public IssueMoney(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.quantity = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        /* Create an instance of the fiat currency token */
        TokenType token = FiatCurrency.Companion.getInstance(currency);

        /* Create an instance of IssuedTokenType for the fiat currency */
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), token);

        /* Create an instance of FungibleToken for the fiat currency to be issued */
        FungibleToken fungibleToken = new FungibleToken(new Amount<>(quantity, issuedTokenType), recipient, null);

        /* Issue the required amount of the token to the recipient */
        return subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(recipient)));
    }
}
