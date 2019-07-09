package corda.tokenSDK.samples.contracts.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.evolvable.CreateEvolvableToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import corda.tokenSDK.samples.states.HouseState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Currency;
import java.util.UUID;

@StartableByRPC
public class HouseTokenCreateAndIssueFlow extends FlowLogic<SignedTransaction> {

    private final Party owner;
    private final Amount<Currency> valuation;
    private final int noOfBedRooms;
    private final String constructionArea;
    private final String additionInfo;
    private final String address;

    public HouseTokenCreateAndIssueFlow(Party owner, Amount<Currency> valuation, int noOfBedRooms, String constructionArea, String additionInfo, String address) {
        this.owner = owner;
        this.valuation = valuation;
        this.noOfBedRooms = noOfBedRooms;
        this.constructionArea = constructionArea;
        this.additionInfo = additionInfo;
        this.address = address;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party issuer = getOurIdentity();
        final HouseState houseState = new HouseState(UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString()), ImmutableList.of(issuer),
                valuation, noOfBedRooms, constructionArea, additionInfo, address);
        subFlow(new CreateEvolvableToken<>(houseState, notary));

        IssuedTokenType<TokenPointer<HouseState>> issuedHouseToken = new IssuedTokenType<>(issuer, houseState.toPointer());
        NonFungibleToken<TokenPointer<HouseState>> houseToken =
                new NonFungibleToken<>(issuedHouseToken, owner, UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString()), TransactionUtilitiesKt.getAttachmentIdForGenericParam(houseState.toPointer()));
        return subFlow(new IssueTokens<>(houseToken));
    }
}
