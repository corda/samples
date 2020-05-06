package com.observable.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class HighlyRegulatedContract implements Contract {
    public static final String ID = "com.observable.contracts.HighlyRegulatedContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // The contract logic is irrelevant for this sample.
    }

    public interface Commands extends CommandData {
        class Trade implements Commands {}
    }

}
