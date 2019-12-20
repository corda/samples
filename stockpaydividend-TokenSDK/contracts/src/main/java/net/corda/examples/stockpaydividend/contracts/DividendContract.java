package net.corda.examples.stockpaydividend.contracts;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.stockpaydividend.states.DividendState;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DividendContract implements Contract {
    public static final String ID = "net.corda.examples.stockpaydividend.contracts.DividendContract";

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
            // Add any validations that may fit
            List<DividendState> outputDividends = tx.outputsOfType(DividendState.class);
            req.using("There must be one output dividend.", outputDividends.size() == 1);

            DividendState outputDividend = outputDividends.get(0);
            req.using("Company and shareholder of the dividend should not be the same.", !outputDividend.getHolder().equals(outputDividend.getIssuer()));
            req.using("Both stock shareholder and company must sign the dividend receivable transaction.", requiredSigners.containsAll(keysFromParticipants(outputDividend)));

            /**
             * A constraint that makes more sense but may fail unless payDay will block running the sample
             */
            // Calendar payDayCal = Calendar.getInstance();
            // payDayCal.setTime(outputDividend.getPayDate());
            // Calendar aWeekLater = Calendar.getInstance();
            // aWeekLater.add(Calendar.DATE, 7);
            // req.using("PayDay should be at least a week after the day of dividend creation", payDayCal.after(aWeekLater));

            return null;
        });
    }

    private void verifyPay(LedgerTransaction tx, List<PublicKey> requiredSigners){
        List<ContractState> inputs = tx.getInputStates();
        requireThat(req -> {
            List<DividendState> inputDividends = tx.inputsOfType(DividendState.class);
            req.using("There must be one input dividend.", inputDividends.size() == 1);

            List<DividendState> outputDividends = tx.outputsOfType(DividendState.class);
            req.using("There should be no output dividends.", outputDividends.isEmpty());

            DividendState inputDividend = inputDividends.get(0);
            req.using("Both stock shareholder and company must sign the dividend receivable transaction.", requiredSigners.containsAll(keysFromParticipants(inputDividend)));
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