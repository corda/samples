package com.heartbeat;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

/**
 * A blank contract and command, solely used for building a valid Heartbeat state transaction.
 */
public class HeartContract implements Contract {
    public final static String contractID = "com.heartbeat.HeartContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Omitted for the purpose of this sample.
    }

    interface Commands extends CommandData {
        class Beat implements Commands {}
    }
}
