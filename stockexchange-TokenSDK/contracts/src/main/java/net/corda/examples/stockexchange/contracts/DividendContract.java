package net.corda.examples.stockexchange.contracts;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.stockexchange.states.DividendState;
import net.corda.examples.stockexchange.states.StockState;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DividendContract implements Contract {
    public static final String ID = "net.corda.examples.stockexchange.contracts.DividendContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), DividendContract.Commands.class);
        List<PublicKey> requiredSigners = command.getSigners();

        if (command.getValue() instanceof DividendContract.Commands.Create) {
            verifyCreate(tx, requiredSigners);
            return;
        } else if (command.getValue() instanceof DividendContract.Commands.Pay) {
            verifyPay(tx, requiredSigners);
            return;
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyCreate(LedgerTransaction tx, List<PublicKey> requiredSigners){
        List<ContractState> outputs = tx.getOutputStates();
        requireThat(req -> {
            //TODO add observer

//            List<StockState> inputStock = tx.outputsOfType(StockState.class);
//            req.using("There must be input stock.", !inputStock.isEmpty());
//
//            List<DividendState> outputStock = tx.outputsOfType(DividendState.class);
//            req.using("There must be output stock.", !outputStock.isEmpty());
//
//            List<DividendState> outputDividends = tx.outputsOfType(DividendState.class);
//            req.using("There must be one output dividend.", outputDividends.size() == 1);
//
//            DividendState outputDividend = outputDividends.get(0);
//            // Checks the required parties have signed.
//            req.using("Both stock holder and issuer must sign the dividend receivable transaction.", requiredSigners.equals(keysFromParticipants(outputDividend)));

            return null;
        });
    }

    private void verifyPay(LedgerTransaction tx, List<PublicKey> requiredSigners){
        List<ContractState> inputs = tx.getInputStates();
        requireThat(req -> {
            //TODO checks for paying off dividend

//            List<DividendState> inputDividends = tx.outputsOfType(DividendState.class);
//            req.using("There must be one input dividend.", inputDividends.size() == 1);
//
//            DividendState inputDividend = inputDividends.get(0);
//             Checks the required parties have signed.
//            req.using("Both stock holder and issuer must sign the dividend receivable transaction.", requiredSigners.equals(keysFromParticipants(inputDividend)));

            return null;
        });

    }

    private Set<PublicKey> keysFromParticipants(DividendState dividend) {
        return dividend
                .getParticipants().stream()
                .map(AbstractParty::getOwningKey)
                .collect(Collectors.toSet());
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Pay implements Commands {}
    }
}