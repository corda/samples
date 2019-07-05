package net.corda.yo;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import org.jetbrains.annotations.NotNull;

public class YoContract implements Contract {
    public static final String ID = "net.corda.yo.YoContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties<Commands.Send> command = requireSingleCommand(tx.getCommands(), Commands.Send.class);
        requireThat(require -> {
            require.using("There can be no inputs when Yo'ing other parties.", tx.getInputs().isEmpty());
            require.using("There must be one output: The Yo!", tx.getOutputs().size() == 1);
            final YoState yo = tx.outputsOfType(YoState.class).get(0);
            require.using("No sending Yo's to yourself!", yo.getTarget() != yo.getOrigin());
            require.using("The Yo! must be signed by the sender", yo.getOrigin().getOwningKey() == command.getSigners().get(0));
            return null;
        });
    }


    public interface Commands extends CommandData {
        class Send extends TypeOnlyCommandData {}
    }
}