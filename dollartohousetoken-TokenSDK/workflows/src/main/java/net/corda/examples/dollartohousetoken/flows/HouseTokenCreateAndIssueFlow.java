package net.corda.examples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.dollartohousetoken.states.HouseState;

import java.util.Arrays;
import java.util.Currency;
import java.util.UUID;

/**
 * Flow to create and issue house token. TokenSDK provides some in-build flows which could be called to Create and Issue tokens.
 * This flow should be called by the issuer of the token. The constructor take the owner and other properties of the house as the as input parameters,
 * it first create the house token onto the issuer's ledger and then issues it to the owner.
**/
@StartableByRPC
public class HouseTokenCreateAndIssueFlow extends FlowLogic<String> {

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
    public String call() throws FlowException {

        /* Choose the notary for the transaction */
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        /* Get a reference of own identity */
        Party issuer = getOurIdentity();

        /* Construct the output state */

        UniqueIdentifier uuid = UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString());
        final HouseState houseState = new HouseState(uuid, Arrays.asList(issuer),
                valuation, noOfBedRooms, constructionArea, additionInfo, address);

        /* Create an instance of TransactionState using the houseState token and the notary */
        TransactionState<HouseState> transactionState = new TransactionState<>(houseState, notary);

        /* Create the house token. TokenSDK provides the CreateEvolvableTokens flow which could be called to create an evolvable token in the ledger.*/
        subFlow(new CreateEvolvableTokens(transactionState));

        /*
        * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner. Note that the IssuedTokenType takes
        * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState. This is done to separate the state info from the token
        * so that the state can evolve independently.
        * IssuedTokenType is a wrapper around the TokenType and the issuer.
        * */
        IssuedTokenType issuedHouseToken = new IssuedTokenType(issuer, houseState.toPointer(HouseState.class));

        /* Create an instance of the non-fungible house token with the owner as the token holder. The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        NonFungibleToken houseToken =
                new NonFungibleToken(issuedHouseToken, owner, UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString()), TransactionUtilitiesKt.getAttachmentIdForGenericParam(houseState.toPointer(HouseState.class)));

        /* Issue the house token by calling the IssueTokens flow provided with the TokenSDK */
        SignedTransaction stx = subFlow(new IssueTokens(Arrays.asList(houseToken)));
        return "\nThe non-fungible house token is created with UUID: "+ uuid +". (This is what you will use in next step)"
                +"\nTransaction ID: "+stx.getId();

    }
}
