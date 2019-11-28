package corda.samples.upgrades.contracts;

import corda.samples.upgrades.states.OldState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class OldContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        //adding some logic so that we can upgrade from v1 to v2. Add your new business logic here.
        OldState oldState = (OldState) tx.getOutputStates().get(0);
        if(oldState.getAmount() <20) throw new IllegalArgumentException("Amount shd be > 20");

    }

    public interface Commands extends CommandData {
        class Issue implements OldContract.Commands {}

    }

}
