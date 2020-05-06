package net.corda.examples.autopayroll.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.autopayroll.states.MoneyState;

import static net.corda.core.contracts.ContractsDSL.*;

// ************
// * Contract *
// ************
public class MoneyStateContract implements Contract {
    // Used to identify our contract when building a transaction.
    public static final String ID = "net.corda.examples.autopayroll.contracts.MoneyStateContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        // Verification logic goes here.
        CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);

        if (cmd.getValue() instanceof Commands.Pay) {
            requireThat(req -> {
                MoneyState output = tx.outputsOfType(MoneyState.class).get(0);
                req.using("Money payment is positive", output.getAmount() > 0);
                return null;
            });
        } else {
            throw new IllegalArgumentException("command not recognized");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Pay implements Commands {}
    }
}
