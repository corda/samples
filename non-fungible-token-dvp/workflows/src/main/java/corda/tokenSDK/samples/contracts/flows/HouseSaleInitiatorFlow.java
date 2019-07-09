package corda.tokenSDK.samples.contracts.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveNonFungibleTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken;
import corda.tokenSDK.samples.states.HouseState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.List;
import java.util.UUID;

@InitiatingFlow
@StartableByRPC
public class HouseSaleInitiatorFlow extends FlowLogic<SignedTransaction> {

    private final String houseId;
    private final Party buyer;

    public HouseSaleInitiatorFlow(String houseId, Party buyer) {
        this.houseId = houseId;
        this.buyer = buyer;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        UUID uuid = UUID.fromString(houseId);
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null, ImmutableList.of(uuid), null, Vault.StateStatus.UNCONSUMED);
        StateAndRef<HouseState> houseStateAndRef = getServiceHub().getVaultService().
                queryBy(HouseState.class, queryCriteria).getStates().get(0);
        HouseState houseState = houseStateAndRef.getState().getData();

        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        MoveTokensUtilitiesKt.addMoveNonFungibleTokens(txBuilder, getServiceHub(), houseState.toPointer(), buyer);

        FlowSession buyerSession = initiateFlow(buyer);
        buyerSession.send(houseState.getValuation());
        List<StateAndRef<FungibleToken<FiatCurrency>>> inputs =  subFlow(new ReceiveStateAndRefFlow<>(buyerSession));
        List<FungibleToken<FiatCurrency>> moneyReceived = buyerSession.receive(List.class).unwrap(value -> value);
        MoveTokensUtilitiesKt.addMoveTokens(txBuilder, inputs, moneyReceived);

        SignedTransaction initialSignedTrnx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());
        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(initialSignedTrnx, ImmutableList.of(buyerSession)));
        subFlow(new UpdateDistributionListFlow(signedTransaction));
        return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(buyerSession)));
    }
}
