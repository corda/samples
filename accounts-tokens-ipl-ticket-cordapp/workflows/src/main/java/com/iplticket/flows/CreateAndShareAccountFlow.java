package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

import java.util.Arrays;

/**
 * This flow will create the account on the node on which you run this flow. This is done using inbuilt flow called CreateAccount.
 * CreateAccount creates an AccountInfo object which has name, host and id as its fields. This is mapped to Account table in the db.
 * For any other party to transact with this account, this AccountInfo will haveto be shared with that Party.
 * Hence the Ipl Ticket Dealers create the ticket buyers accounts on their end and share this accountInfo with the Bank node and BCCI node.
 */
@StartableByRPC
@InitiatingFlow
public class CreateAndShareAccountFlow extends FlowLogic {

    private final String accountName;
    private final Party partyToShareAccountInfoTo;//TODO change this to a list

    public CreateAndShareAccountFlow(String accountName, Party partyToShareAccountInfoTo) {
        this.accountName = accountName;
        this.partyToShareAccountInfoTo = partyToShareAccountInfoTo;
    }

    @Override
    @Suspendable
    public Object call() throws FlowException {

        //Call inbuilt CreateAccount flow to create the AccountInfo object
        StateAndRef<AccountInfo> accountInfoStateAndRef = (StateAndRef<AccountInfo>) subFlow(new CreateAccount(accountName));

        //Share this AccountInfo object with the parties who want to transact with this account
        return subFlow(new ShareAccountInfo(accountInfoStateAndRef, Arrays.asList(partyToShareAccountInfoTo)));
    }
}
