package net.corda.examples.whistleblower.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.whistleblower.states.BlowWhistleState;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.*;

/**
 * A contract supporting two state transitions:
 * - Blowing the whistle on a company
 * - Transferring an existing case to a new investigator
 */
public class BlowWhistleContract implements Contract {
    public final static String ID = "net.corda.examples.whistleblower.contracts.BlowWhistleContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);

        if (cmd.getValue() instanceof Commands.BlowWhistleCmd) {
            requireThat(req -> {
                req.using("A BlowWhistle transaction should have zero inputs.", tx.getInputs().isEmpty());
                req.using("A BlowWhistle transaction should have a BlowWhistleState output.", tx.outputsOfType(BlowWhistleState.class).size() == 1);
                req.using("A BlowWhistle transaction should have no other outputs.", tx.getOutputs().size() == 1);

                BlowWhistleState output = tx.outputsOfType(BlowWhistleState.class).get(0);
                req.using("A BlowWhistle transaction should be signed by the whistle-blower and the investigator.", cmd.getSigners().containsAll(
                        output.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())
                ));
                return null;
            });
        } else {
            throw new IllegalArgumentException("command not recognised");
        }
    }

    public interface Commands extends CommandData {
        /** Blowing the whistle on a company. */
        class BlowWhistleCmd implements Commands {}
    }
}
