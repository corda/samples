package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.node.services.IdentityService;
import net.corda.examples.stockexchange.flows.utilities.ObserversUtilities;
import net.corda.examples.stockexchange.states.StockState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class IssueStock extends FlowLogic<SignedTransaction> {

    private String symbol;
    private String name;
    private String currency;
    private int issueVol;

    // Using NetworkmapCache.getNotaryIdentities().get(0) is not encouraged due to multi notary is introduced
    private Party notary;

    public IssueStock(String symbol, String name, String currency, int issueVol, Party notary) {
        this.symbol = symbol;
        this.name = name;
        this.currency = currency;
        this.issueVol = issueVol;
        this.notary = notary;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        IdentityService identityService = getServiceHub().getIdentityService();

        List<Party> observers = ObserversUtilities.getLegalIdenties(identityService);

        // Get a reference of own identity
        Party issuer = getOurIdentity();

        // Construct the output state
        final StockState stockState = new StockState(new UniqueIdentifier(), ImmutableList.of(issuer),
                symbol, name, currency, BigDecimal.valueOf(0), new Date(), new Date()
                );

        TransactionState<StockState> transactionState = new TransactionState<>(stockState, notary);

        subFlow(new CreateEvolvableTokens(transactionState, observers));

        IssuedTokenType issuedStock = new IssuedTokenType(issuer, stockState.toPointer());

        Amount<IssuedTokenType> issueAmount = new Amount(new Long(issueVol), issuedStock);

        FungibleToken stockToken = new FungibleToken(issueAmount, getOurIdentity(), null);

        return subFlow(new IssueTokens(ImmutableList.of(stockToken), observers));
    }
}
