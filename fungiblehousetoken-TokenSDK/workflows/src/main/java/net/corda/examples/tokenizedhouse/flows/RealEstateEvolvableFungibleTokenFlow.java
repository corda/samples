package net.corda.examples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.*;
import kotlin.Unit;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.tokenizedhouse.states.FungibleHouseTokenState;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Create,Issue,Move,Redeem token flows for a house asset on ledger
 */
public class RealEstateEvolvableFungibleTokenFlow {

    private RealEstateEvolvableFungibleTokenFlow() {
        //Instantiation not allowed
    }

    /**
     * Create Fungible Token for a house asset on ledger
     */
    @StartableByRPC
    public static class CreateHouseTokenFlow extends FlowLogic<SignedTransaction> {

        // valuation property of a house can change hence we are considering house as a evolvable asset
        private final BigDecimal valuation;
        private final String symbol;


        public CreateHouseTokenFlow(String symbol, BigDecimal valuation) {
            this.valuation = valuation;
            this.symbol = symbol;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //grab the notary
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //create token type
            FungibleHouseTokenState evolvableTokenType = new FungibleHouseTokenState(valuation, getOurIdentity(),
                    new UniqueIdentifier(), 0, this.symbol);

            //warp it with transaction state specifying the notary
            TransactionState transactionState = new TransactionState(evolvableTokenType, notary);

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new CreateEvolvableTokens(transactionState));

        }
    }

    /**
     *  Issue Fungible Token against an evolvable house asset on ledger
     */
    @StartableByRPC
    public static class IssueHouseTokenFlow extends FlowLogic<SignedTransaction>{
        private final String symbol;
        private final int quantity;
        private final Party holder;

        public IssueHouseTokenFlow(String symbol, int quantity, Party holder) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.holder = holder;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleHouseTokenState evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //assign the issuer to the house type who will be issuing the tokens
            IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), tokenPointer);

            //specify how much amount to issue to holder
            Amount<IssuedTokenType> amount = new Amount(quantity, issuedTokenType);

            //create fungible amount specifying the new owner
            FungibleToken fungibleToken  = new FungibleToken(amount, holder, TransactionUtilitiesKt.getAttachmentIdForGenericParam(tokenPointer));

            //use built in flow for issuing tokens on ledger
            return subFlow(new IssueTokens(Arrays.asList(fungibleToken)));
        }
    }

    /**
     *  Move created fungible tokens to other party
     */
    @StartableByRPC
    @InitiatingFlow
    public static class MoveHouseTokenFlow extends FlowLogic<SignedTransaction>{
        private final String symbol;
        private final Party holder;
        private final int quantity;


        public MoveHouseTokenFlow(String symbol, Party holder, int quantity) {
            this.symbol = symbol;
            this.holder = holder;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleHouseTokenState tokenstate = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer<FungibleHouseTokenState> tokenPointer = tokenstate.toPointer(FungibleHouseTokenState.class);

            //specify how much amount to transfer to which holder
            Amount<TokenType> amount = new Amount(quantity, tokenPointer);
            //PartyAndAmount partyAndAmount = new PartyAndAmount(holder, amount);

            //use built in flow to move fungible tokens to holder
            return subFlow(new MoveFungibleTokens(amount,holder));
        }
    }

    @InitiatedBy(MoveHouseTokenFlow.class)
    public static class MoveEvolvableFungibleTokenFlowResponder extends FlowLogic<Unit>{

        private FlowSession counterSession;

        public MoveEvolvableFungibleTokenFlowResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }

    /**
     *  Holder Redeems fungible token issued by issuer. The code below is a demonstration for how to redeem a toke.
     *
     *  Or we have to define an issuance celling for the fungible token,
     *  and you can redeem for the non-fungible asset, the house in this case, when you have all the fungible tokens.
     */
    @StartableByRPC
    public static class RedeemHouseFungibleTokenFlow extends FlowLogic<SignedTransaction> {

        private final String symbol;
        private final Party issuer;
        private final int quantity;

        public RedeemHouseFungibleTokenFlow(String symbol, Party issuer, int quantity) {
            this.symbol = symbol;
            this.issuer = issuer;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleHouseTokenState evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //specify how much amount quantity of tokens of type token parameter
            Amount amount = new Amount(quantity, tokenPointer);

            //call built in redeem flow to redeem tokens with issuer
            return subFlow(new RedeemFungibleTokens(amount, issuer));
        }
    }
}

