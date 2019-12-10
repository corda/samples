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

    public IssueMoney(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.quantity = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        // Create an instance of the fiat currency token type
        TokenType token = FiatCurrency.Companion.getInstance(currency);

        // Create an instance of IssuedTokenType which represents the token is issued by this party
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), token);

        // Create an instance of FungibleToken for the fiat currency to be issued
        FungibleToken fungibleToken = new FungibleToken(new Amount<>(quantity, issuedTokenType), recipient, null);

        // Use the build-in flow, IssueTokens, to issue the required amount to the the recipient
        return subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(recipient)));
    }
}