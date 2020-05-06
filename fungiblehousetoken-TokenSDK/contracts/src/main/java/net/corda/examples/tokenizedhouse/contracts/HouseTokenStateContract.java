package net.corda.examples.tokenizedhouse.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
public class HouseTokenStateContract extends EvolvableTokenContract implements Contract {

    @Override
    public void additionalCreateChecks(LedgerTransaction tx) {
        // add additional create checks here
    }

    @Override
    public void additionalUpdateChecks(LedgerTransaction tx) {
        // add additional update checks here
    }
}
