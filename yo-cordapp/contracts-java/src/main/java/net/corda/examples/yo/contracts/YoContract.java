package net.corda.examples.yo.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.yo.states.YoState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.*;

// ************
// * Contract *
// ************
// Contract and state.
public class YoContract implements Contract {
    // Used to identify our contract when building a transaction.
    public static final String ID = "net.corda.examples.yo.contracts.YoContract";

    // Contract code.
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands.Send> command = requireSingleCommand(tx.getCommands(), Commands.Send.class);
        requireThat(req -> {
            req.using("There can be no inputs when Yo'ing other parties", tx.getInputs().isEmpty());
            req.using("There must be one output: The Yo!", tx.getOutputs().size() == 1);
            YoState yo = tx.outputsOfType(YoState.class).get(0);
            req.using("No sending Yo's to yourself!", !yo.getTarget().equals(yo.getOrigin()));
            req.using("The Yo! must be signed by the sender.", command.getSigners().contains(yo.getOrigin().getOwningKey()));
            return null;
        });
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Send implements Commands {}
    }
}
