package net.corda.examples.sendfile.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.sendfile.states.InvoiceState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.*;
// ************
// * Contract *
// ************
public class InvoiceContract implements Contract {
    // Used to identify our contract when building a transaction.
    public final static String ID = "net.corda.examples.sendfile.contracts.InvoiceContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands> commandData = requireSingleCommand(tx.getCommands(), Commands.class);

        if (commandData.getValue() instanceof Commands.Issue) {
            requireThat(req -> {
                List<InvoiceState> output = tx.outputsOfType(InvoiceState.class);
                req.using("must be single output", output.size() == 1);
                req.using("Attachment ID must be stored in state", !output.get(0).getInvoiceAttachmentID().isEmpty());
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Issue implements Commands {}
    }
}
