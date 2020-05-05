package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.template.states.IplTicket;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Arrays;
import java.util.UUID;

/**
 * This will be run by the BCCI node and it will issue a nonfungible token represnting each ticket to the dealer account.
 * Buyers can then buy tickets from the dealer account.
 */
@StartableByRPC
@InitiatingFlow
public class IssueNonFungibleTicketFlow extends FlowLogic {

    private final String tokenId;
    //private final int quantity;//TODO handle quantity later
    private final String dealerAccountName;

    public IssueNonFungibleTicketFlow(String tokenId, String dealerAccountName) {
        this.tokenId = tokenId;
       // this.quantity = quantity;
        this.dealerAccountName = dealerAccountName;
    }


    @Override
    @Suspendable
    public Object call() throws FlowException {

        //Since dealer has already shared the dealer account with BCCI, BCCI will retrieve this accountinfo from the vault.
        AccountInfo dealerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(dealerAccountName).get(0).getState().getData();

        //Generate the key pair for dealer account so taht BCCI node will be able to transact with issue a token to the dealer account.
        AnonymousParty dealerAccount = (AnonymousParty) subFlow(new RequestKeyForAccount(dealerAccountInfo));

        UUID uuid = UUID.fromString(tokenId);

        //construct the query criteria and get the base token type
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(uuid), null,
                Vault.StateStatus.UNCONSUMED, null);

        //grab the created ticket type off the ledger
        StateAndRef<IplTicket> stateAndRef = getServiceHub().getVaultService().
                queryBy(IplTicket.class, queryCriteria).getStates().get(0);


        IplTicket evolvableTokenType = stateAndRef.getState().getData();

        //get the pointer pointer to the IplTicket
        TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

        //assign the issuer to the IplTicket type who is the BCCI node
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), tokenPointer);

        //mention the current holder which is now going to be the dealer account
        NonFungibleToken nonFungibleToken = new NonFungibleToken(issuedTokenType, dealerAccount, new UniqueIdentifier(),
                TransactionUtilitiesKt.getAttachmentIdForGenericParam(tokenPointer));

        //call built in flow to issue non fungible tokens
        return subFlow(new IssueTokens(Arrays.asList(nonFungibleToken)));
    }
}
