package net.corda.samples.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class AssetContract implements Contract {
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Verification code goes here. Left blank for simplicity
    }

    public interface Commands extends CommandData {
        class CreateAsset implements Commands {}
        class TransferAsset implements Commands {}
    }
}
