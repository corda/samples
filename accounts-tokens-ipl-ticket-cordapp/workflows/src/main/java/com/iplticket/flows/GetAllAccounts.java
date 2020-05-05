package com.template.flows;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

import java.util.ArrayList;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class GetAllAccounts extends FlowLogic<List<AccountInfo>> {

    @Override
    public List<AccountInfo> call() throws FlowException {
        AccountService accountService = UtilitiesKt.getAccountService(this);

        List<StateAndRef<AccountInfo>> stateAndRefList =  accountService.allAccounts();

        List<AccountInfo> accountInfoList = new ArrayList<>();


        for(StateAndRef<AccountInfo> stateAndRef : stateAndRefList) {
            accountInfoList.add(stateAndRef.getState().getData());
        }

        return accountInfoList;

    }
}
