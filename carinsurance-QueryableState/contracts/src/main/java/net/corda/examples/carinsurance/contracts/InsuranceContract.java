package net.corda.examples.carinsurance.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class InsuranceContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.examples.carinsurance.contracts.InsuranceContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), InsuranceContract.Commands.class);
        List<ContractState> inputs = tx.getInputStates();
        if (command.getValue() instanceof InsuranceContract.Commands.IssueInsurance) {
            requireThat(req -> {
                req.using("Transaction must have no input states.", inputs.isEmpty());
                return null;
            });
        } else if(command.getValue() instanceof InsuranceContract.Commands.AddClaim){
            requireThat(req -> {
                req.using("Insurance transaction must have input states, the insurance police", (!inputs.isEmpty()));
                return null;
            });
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class IssueInsurance implements Commands {}
        class AddClaim implements Commands {}
    }
}