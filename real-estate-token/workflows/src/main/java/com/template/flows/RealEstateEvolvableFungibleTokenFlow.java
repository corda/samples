package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.*;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.template.states.RealEstateEvolvableTokenType;
import net.corda.core.contracts.*;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;

import java.math.BigDecimal;
import java.util.UUID;

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
    public static class CreateEvolvableFungibleTokenFlow extends FlowLogic<SignedTransaction> {

        // valuation property of a house can change hence we are considering house as a evolvable asset
        private final BigDecimal valuation;

        public CreateEvolvableFungibleTokenFlow(BigDecimal valuation) {
            this.valuation = valuation;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //grab the notary
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //create token type
            RealEstateEvolvableTokenType evolvableTokenType = new RealEstateEvolvableTokenType(valuation, getOurIdentity(),
                    new UniqueIdentifier(), 0);

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
    public static class IssueEvolvableFungibleTokenFlow extends FlowLogic<SignedTransaction>{
        private final String tokenId;
        private final int quantity;
        private final Party holder;

        public IssueEvolvableFungibleTokenFlow(String tokenId, int quantity, Party holder) {
            this.tokenId = tokenId;
            this.quantity = quantity;
            this.holder = holder;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get uuid from input tokenId
            UUID uuid = UUID.fromString(tokenId);

            //create criteria to get all unconsumed house states on ledger with uuid as input tokenId
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED);
            StateAndRef<RealEstateEvolvableTokenType> stateAndRef = getServiceHub().getVaultService().
                    queryBy(RealEstateEvolvableTokenType.class, queryCriteria).getStates().get(0);

            //get the RealEstateEvolvableTokenType object
            RealEstateEvolvableTokenType evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //assign the issuer to the house type who will be issuing the tokens
            IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), tokenPointer);

            //specify how much amount to issue to holder
            Amount<IssuedTokenType> amount = new Amount(quantity, issuedTokenType);

            //create fungible amount specifying the new owner
            FungibleToken fungibleToken  = new FungibleToken(amount, holder, TransactionUtilitiesKt.getAttachmentIdForGenericParam(tokenPointer));

            //use built in flow for issuing tokens on ledger
            return subFlow(new IssueTokens(ImmutableList.of(fungibleToken)));
        }
    }

    /**
     *  Move created fungible tokens to other party
     */
    @StartableByRPC
    public static class MoveEvolvableFungibleTokenFlow extends FlowLogic<SignedTransaction>{
        private final String tokenId;
        private final Party holder;
        private final int quantity;


        public MoveEvolvableFungibleTokenFlow(String tokenId, Party holder, int quantity) {
            this.tokenId = tokenId;
            this.holder = holder;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get uuid from input tokenId
            UUID uuid = UUID.fromString(tokenId);

            //create criteria to get all unconsumed house states on ledger with uuid as input tokenId
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null);
            StateAndRef<RealEstateEvolvableTokenType> stateAndRef = getServiceHub().getVaultService().
                    queryBy(RealEstateEvolvableTokenType.class, queryCriteria).getStates().get(0);

            //get the RealEstateEvolvableTokenType object
            RealEstateEvolvableTokenType evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //specify how much amount to transfer to which holder
            Amount<TokenPointer> amount = new Amount(quantity, tokenPointer);
            PartyAndAmount partyAndAmount = new PartyAndAmount(holder, amount);

            //use built in flow to move fungible tokens to holder
            return subFlow(new MoveFungibleTokens(partyAndAmount));
        }
    }

    /**
     *  Holder Redeems fungible token issued by issuer
     */
    @StartableByRPC
    public static class RedeemHouseFungibleTokenFlow extends FlowLogic<SignedTransaction> {

        private final String tokenId;
        private final Party issuer;
        private final int quantity;

        public RedeemHouseFungibleTokenFlow(String tokenId, Party issuer, int quantity) {
            this.tokenId = tokenId;
            this.issuer = issuer;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get uuid from input tokenId
            UUID uuid = UUID.fromString(tokenId);

            //create criteria to get all unconsumed house states on ledger with uuid as input tokenId
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null);
            StateAndRef<RealEstateEvolvableTokenType> stateAndRef = getServiceHub().getVaultService().
                    queryBy(RealEstateEvolvableTokenType.class, queryCriteria).getStates().get(0);

            //get the RealEstateEvolvableTokenType object
            RealEstateEvolvableTokenType evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //specify how much amount quantity of tokens of type token parameter
            Amount amount = new Amount(quantity, tokenPointer);

            //call built in redeem flow to redeem tokens with issuer
            return subFlow(new RedeemFungibleTokens(amount, issuer));
        }
    }
}

