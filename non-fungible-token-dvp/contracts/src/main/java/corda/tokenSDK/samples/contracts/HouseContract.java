package corda.tokenSDK.samples.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;


public class HouseContract extends EvolvableTokenContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while creation of token
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while updation of token
    }

}
