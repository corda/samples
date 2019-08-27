package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken;
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

public class RealEstateEvolvableNonFungibleTokenFlow {

    private RealEstateEvolvableNonFungibleTokenFlow() {
        //Instantiation not allowed
    }

    /**
     * Create NonFungible Token in ledger
     */
    @StartableByRPC
    public static class CreateEvolvableTokenFlow extends FlowLogic<SignedTransaction> {

        private final BigDecimal valuation;

        public CreateEvolvableTokenFlow(BigDecimal valuation) {
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
            return (SignedTransaction) subFlow(new CreateEvolvableTokens(transactionState));
        }
    }

    /**
     *  Issue Non Fungible Token
     */
    @StartableByRPC
    public static class IssueEvolvableTokenFlow extends FlowLogic<SignedTransaction>{
        private final String tokenId;
        private final Party holder;

        public IssueEvolvableTokenFlow(String tokenId, Party recipient) {
            this.tokenId = tokenId;
            this.holder = recipient;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //using id of my house to grab the house from db.
            // you can use any custom criteria depending on your requirements
            UUID uuid = UUID.fromString(tokenId);

            //construct the query criteria
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null);

            // grab the house off the ledger
            StateAndRef<RealEstateEvolvableTokenType> stateAndRef = getServiceHub().getVaultService().
                    queryBy(RealEstateEvolvableTokenType.class, queryCriteria).getStates().get(0);
            RealEstateEvolvableTokenType evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //assign the issuer to the house type who will be issuing the tokens
            IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), tokenPointer);

            //mention the current holder also
            NonFungibleToken nonFungibleToken = new NonFungibleToken(issuedTokenType, holder, new UniqueIdentifier(), TransactionUtilitiesKt.getAttachmentIdForGenericParam(tokenPointer));

            //call built in flow to issue non fungible tokens
            return (SignedTransaction) subFlow(new IssueTokens(ImmutableList.of(nonFungibleToken)));
        }
    }

    /**
     *  Move created non fungible token to other party
     */
    @StartableByRPC
    public static class MoveEvolvableTokenFlow extends FlowLogic<SignedTransaction>{
        private final String tokenId;
        private final Party holder;


        public MoveEvolvableTokenFlow(String tokenId, Party recipient) {
            this.tokenId = tokenId;
            this.holder = recipient;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //using id of my house to grab the house from db.
            //you can use any custom criteria depending on your requirements
            UUID uuid = UUID.fromString(tokenId);

            //construct the query criteria
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null);

            // grab the house off the ledger
            StateAndRef<RealEstateEvolvableTokenType> stateAndRef = getServiceHub().getVaultService().
                    queryBy(RealEstateEvolvableTokenType.class, queryCriteria).getStates().get(0);
            RealEstateEvolvableTokenType evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //specify the party who will be the new owner of the token
            PartyAndToken partyAndToken = new PartyAndToken(holder, tokenPointer);
            return (SignedTransaction) subFlow(new MoveNonFungibleTokens(partyAndToken));
        }
    }

    /**
     *  Holder Redeems non fungible token issued by issuer
     */
    @StartableByRPC
    public static class RedeemHouseToken extends FlowLogic<SignedTransaction> {

        private final String tokenId;
        private final Party issuer;

        public RedeemHouseToken(String tokenId, Party issuer) {
            this.tokenId = tokenId;
            this.issuer = issuer;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //using id of my house to grab the house from db.
            //you can use any custom criteria depending on your requirements
            UUID uuid = UUID.fromString(tokenId);

            //construct the query criteria
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null);

            // grab the house off the ledger
            StateAndRef<RealEstateEvolvableTokenType> stateAndRef = getServiceHub().getVaultService().
                    queryBy(RealEstateEvolvableTokenType.class, queryCriteria).getStates().get(0);
            RealEstateEvolvableTokenType evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer token =  evolvableTokenType.toPointer(evolvableTokenType.getClass());

            //call built in flow to redeem the tokens
            return (SignedTransaction) subFlow(new RedeemNonFungibleTokens(token, issuer));
        }
    }
}

