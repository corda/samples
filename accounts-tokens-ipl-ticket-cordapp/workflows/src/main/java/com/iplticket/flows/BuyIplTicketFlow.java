package com.template.flows;


import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.template.states.IplTicket;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;


/**
 * This is the DVP flow, where the buyer account buys the ticket token from the dealer account and in turn transfers him cash worth of the ticket.
 * Once buyer1 buys the token from the dealer, he can further sell this ticket to other buyers.
 * Note : this flow handles dvp from account to account on same node. This flow later will be modified if a buyer on dealer1 node wants to buy ticket from
 * dealer2 node.
 */
@StartableByRPC
@InitiatingFlow
public class BuyIplTicketFlow extends FlowLogic<Void> {

    private final String tokenId;
    private final String buyerAccountName;
    private final String sellerAccountName;
    private final String currency;
    private final Long costOfTicket;

    public BuyIplTicketFlow(String tokenId, String buyerAccountName, String sellerAccountName, Long costOfTicket, String currency) {
        this.tokenId = tokenId;
        this.buyerAccountName = buyerAccountName;
        this.sellerAccountName = sellerAccountName;
        this.costOfTicket = costOfTicket;
        this.currency = currency;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        //Get buyers and sellers account infos
        AccountInfo buyerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(buyerAccountName).get(0).getState().getData();

        AccountInfo sellerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(sellerAccountName).get(0).getState().getData();

        //Generate new keys for buyers and sellers
        AnonymousParty buyerAccount = subFlow(new RequestKeyForAccount(buyerAccountInfo));

        AnonymousParty sellerAccount = subFlow(new RequestKeyForAccount(sellerAccountInfo));

        UUID uuid = UUID.fromString(tokenId);

        //Part1 : Move non fungible token - ticket from seller to buyer

        //construct the query criteria and get the base token type
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(uuid), null,
                Vault.StateStatus.UNCONSUMED, null);

        // grab the house off the ledger
        StateAndRef<IplTicket> stateAndRef = getServiceHub().getVaultService().
                queryBy(IplTicket.class, queryCriteria).getStates().get(0);

        IplTicket evolvableTokenType = stateAndRef.getState().getData();

        //get the pointer pointer to the IplTicket
        TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        //first part of DVP is to transfer the non fungible token from seller to buyer
        MoveTokensUtilitiesKt.addMoveNonFungibleTokens(transactionBuilder, getServiceHub(), tokenPointer, buyerAccount);

        //Part2 : Move fungible token - cash from buyer to seller

        Amount<FiatCurrency> amount = new Amount(costOfTicket, FiatCurrency.Companion.getInstance(currency));

        //move money to sellerAccountInfo account.
        PartyAndAmount partyAndAmount = new PartyAndAmount(sellerAccount, amount);

        //construct the query criteria and get all available unconsumed fungible tokens which belong to buyers account
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED).
                withExternalIds(Arrays.asList(buyerAccountInfo.getIdentifier().getId()));

        //call utility function to move the fungible token from buyer to seller account
        MoveTokensUtilitiesKt.addMoveFungibleTokens(transactionBuilder, getServiceHub(), Arrays.asList(partyAndAmount), buyerAccount, criteria);

        //self sign the transaction. note : the host party will first self sign the transaction.
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder,
                Arrays.asList(getOurIdentity().getOwningKey()));

        //establish sessions with buyer and seller. to establish session get the host name from accountinfo object
        FlowSession customerSession = initiateFlow(buyerAccountInfo.getHost());

        FlowSession dealerSession = initiateFlow(sellerAccountInfo.getHost());

        //Note: though buyer and seller are on the same node still we will have to call CollectSignaturesFlow as the signer is not a Party but an account.
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(selfSignedTransaction,
                Arrays.asList(customerSession, dealerSession)));

        //call ObserverAwareFinalityFlow for finality
        subFlow(new ObserverAwareFinalityFlow(fullySignedTx, Arrays.asList(customerSession, dealerSession)));

        return null;
    }
}

@InitiatedBy(BuyIplTicketFlow.class)
class BuyIplTicketFlowResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public BuyIplTicketFlowResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        subFlow(new SignTransactionFlow(otherSide) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });

        subFlow(new ReceiveFinalityFlow(otherSide));

        return null;
    }
}

