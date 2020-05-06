package net.corda.examples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.dollartohousetoken.states.HouseState;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Initiator Flow class to propose the sale of the house. The house token would be exchanged with an equivalent amount of fiat currency as mentioned in the
 * valuation of the house. The flow taken the linearId of the house token and the buyer party as the input parameters.
 * */
@InitiatingFlow
@StartableByRPC
public class HouseSaleInitiatorFlow extends FlowLogic<String> {

    private final String houseId;
    private final Party buyer;

    public HouseSaleInitiatorFlow(String houseId, Party buyer) {
        this.houseId = houseId;
        this.buyer = buyer;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        /* Choose the notary for the transaction */
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        UUID uuid = UUID.fromString(houseId);

        /* Fetch the house state from the vault using the vault query */
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null, Arrays.asList(uuid), null, Vault.StateStatus.UNCONSUMED);
        StateAndRef<HouseState> houseStateAndRef = getServiceHub().getVaultService().
                queryBy(HouseState.class, queryCriteria).getStates().get(0);
        HouseState houseState = houseStateAndRef.getState().getData();

        /* Build the transaction builder */
        TransactionBuilder txBuilder = new TransactionBuilder(notary);

        /* Create a move token proposal for the house token using the helper function provided by Token SDK. This would create the movement proposal and would
         * be committed in the ledgers of parties once the transaction in finalized.
        **/
        MoveTokensUtilitiesKt.addMoveNonFungibleTokens(txBuilder, getServiceHub(), houseState.toPointer(HouseState.class), buyer);

        /* Initiate a flow session with the buyer to send the house valuation and transfer of the fiat currency */
        FlowSession buyerSession = initiateFlow(buyer);
        // Send the house valuation to the buyer.
        buyerSession.send(houseState.getValuation());
        // Recieve inputStatesAndRef for the fiat currency exchange from the buyer, these would be inputs to the fiat currency exchange transaction.
        List<StateAndRef<FungibleToken>> inputs =  subFlow(new ReceiveStateAndRefFlow<>(buyerSession));
        // Recieve output for the fiat currency from the buyer, this would contain the transfered amount from buyer to yourself
        List<FungibleToken> moneyReceived = buyerSession.receive(List.class).unwrap(value -> value);

        /* Create a fiat currency proposal for the house token using the helper function provided by Token SDK. */
        MoveTokensUtilitiesKt.addMoveTokens(txBuilder, inputs, moneyReceived);

        /* Sign the transaction with your private */
        SignedTransaction initialSignedTrnx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());
        /* Call the CollectSignaturesFlow to recieve signature of the buyer */
        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(initialSignedTrnx, Arrays.asList(buyerSession)));
        /* Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow */
        subFlow(new UpdateDistributionListFlow(signedTransaction));
        /* Call finality flow to notarise the transaction */
        SignedTransaction stx = subFlow(new FinalityFlow(signedTransaction, Arrays.asList(buyerSession)));
        return "\nThe house is sold to "+ this.buyer.getName().getOrganisation() + "\nTransaction ID: "
                + stx.getId();
    }
}
