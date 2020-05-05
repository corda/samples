package com.template.webserver;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.template.flows.GetAllAccounts;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/")
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    @GetMapping(value = "/all-accounts")
    private List<String> getAllAccounts() {
        List<AccountInfo>  result = null;
        try {
            result = this.proxy.startTrackedFlowDynamic(GetAllAccounts.class).getReturnValue().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        List<String> accountInfoNames = new ArrayList<>();

        for(AccountInfo accountInfo : result) {
            accountInfoNames.add(accountInfo.getName());
        }

        return accountInfoNames;
    }

    @PostMapping(value = "/cash-balance")
    private Long getCashBalanceForAccount(String accountId) {

        UUID uuid = UUID.fromString(accountId);

        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED).
                withExternalIds(Arrays.asList(uuid));

        List<StateAndRef<FungibleToken>>  list = this.proxy.vaultQueryByCriteria(criteria, FungibleToken.class).getStates();

        Long totalBalance = 0L;

        for(StateAndRef<FungibleToken> stateAndRef : list) {
            totalBalance += stateAndRef.getState().getData().getAmount().getQuantity();
        }

        return totalBalance;
    }
}