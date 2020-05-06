package net.corda.examples.bikemarket.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class FrameContract extends EvolvableTokenContract implements Contract {
    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {

    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {

    }
}
