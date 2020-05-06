package com.example.contract;

import com.example.state.SanctionedEntities;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SanctionedEntitiesContract implements Contract {
    public static String SANCTIONS_CONTRACT_ID = "com.example.contract.SanctionedEntitiesContract";


    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        Command command = tx.commandsOfType(Commands.class).get(0);

        if(command.getValue() instanceof  Commands.Create){
            requireThat(require -> {
                require.using("when creating a sanctions list there should be no inputs", tx.getInputStates().isEmpty());
                require.using("when creating a sanctions list there should be one output", tx.outputsOfType(SanctionedEntities.class).size() == 1);
                SanctionedEntities out = tx.outputsOfType(SanctionedEntities.class).get(0);
                require.using("The issuer of the sanctions list must sign", command.getSigners().contains(out.getIssuer().getOwningKey()));
                return null;
            }) ;
        }else if(command.getValue() instanceof Commands.Update){
            requireThat(require -> {
                require.using("There must be exactly one input Sanctions List when updating", tx.inputsOfType(SanctionedEntities.class).size() == 1);
                require.using("There must be exactly one output Sanctions List when updating", tx.outputsOfType(SanctionedEntities.class).size() == 1);
                SanctionedEntities input = tx.inputsOfType(SanctionedEntities.class).get(0);
                SanctionedEntities output = tx.outputsOfType(SanctionedEntities.class).get(0);
                require.using("The issuer must remain the same across an update", input.getIssuer().equals(output.getIssuer()));
                return null;
            });
        }
    }


    public interface Commands extends CommandData {
        class Create implements Commands {};
        class Update implements  Commands {};
    }
}
