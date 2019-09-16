package corda.samples.upgrades.contracts;

import corda.samples.upgrades.states.TenderState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class TenderContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if(tx.getCommand(0).getValue() instanceof Commands.CreateAndPublish) {
            if(tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("We are publishing tender so exactly one output should be created");
            if(tx.getInputStates().size() != 0)
                throw new IllegalArgumentException("Tender is published so there shouldnt be any input states");
            //below new condition is added in TenderContract version 2
            TenderState refState = (TenderState) tx.getOutputStates().get(0);
            if(!tx.getCommands().get(0).getSigners().contains(refState.getTenderingOrganisation().getOwningKey()))
                throw new IllegalArgumentException("Tendering Organisation should be the signing party");
        }
    }

    public interface Commands extends CommandData {
        class CreateAndPublish implements Commands {}
    }
}
