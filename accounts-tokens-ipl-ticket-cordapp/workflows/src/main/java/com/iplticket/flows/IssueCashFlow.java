package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;

import java.util.Arrays;
import java.util.Currency;

/**
 * This should be run by the bank node. The Bank node issues Cash (Fungible Tokens) to the buyers who want to buy he Ipl ticket.
 */

@StartableByRPC
@InitiatingFlow
public class IssueCashFlow extends FlowLogic {

    private final String accountName;
    private final String currency;
    private final Long amount;

    public IssueCashFlow(String accountName, String currency, Long amount) {
        this.accountName = accountName;
        this.currency = currency;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public Object call() throws FlowException {

        //Dealer node has already shared accountinfo with the bank when we ran the CreateAndShareAccountFlow. So this bank node will
        //have access to AccountInfo of the buyer. Retrieve it using the AccountService. AccountService has certain helper methods, take a look at them.
        AccountInfo accountInfo = UtilitiesKt.getAccountService(this).accountInfo(accountName).get(0).getState().getData();

        //To transact with any account, we have to request for a Key from the node hosting the account. For this we use RequestKeyForAccount inbuilt flow.
        //This will return a Public key wrapped in an AnonymousParty class.
        AnonymousParty anonymousParty = (AnonymousParty) subFlow(new RequestKeyForAccount(accountInfo));

        //Get the base token type for issuing fungible tokens
        TokenType token = getInstance(currency);

        //issuer will be the bank. Keep in mind the issuer will always be an known legal Party class and not an AnonymousParty. This is by design
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), token);

        //Create a fungible token for issuing cash to account
        FungibleToken fungibleToken = new FungibleToken(new Amount(this.amount, issuedTokenType), anonymousParty, null);

        //Issue fungible tokens to specified account
        subFlow(new IssueTokens(Arrays.asList(fungibleToken)));

        return null;

    }

    public TokenType getInstance(String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        return new TokenType(currency.getCurrencyCode(), 0);
    }
}


