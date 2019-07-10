package corda.tokenSDK.samples.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import corda.tokenSDK.samples.states.HouseState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;


public class HouseContract extends EvolvableTokenContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        HouseState outputState = (HouseState) tx.getOutput(0);
        if(!(tx.getCommand(0).getSigners().contains(outputState.getIssuer().getOwningKey())))
            throw new IllegalArgumentException("Issuer Signature Required");
    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while creation of token
        HouseState outputState = (HouseState) tx.getOutput(0);
        if(outputState.getValuation().getQuantity() < 1)
            throw new IllegalArgumentException("Valuation must be greater than zero");
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while updation of token
    }

}
