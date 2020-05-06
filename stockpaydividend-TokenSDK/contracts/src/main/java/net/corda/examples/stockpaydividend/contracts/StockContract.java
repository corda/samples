package net.corda.examples.stockpaydividend.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.examples.stockpaydividend.states.StockState;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class StockContract extends EvolvableTokenContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        StockState outputState = (StockState) tx.getOutput(0);
        if(!(tx.getCommand(0).getSigners().contains(outputState.getIssuer().getOwningKey())))
            throw new IllegalArgumentException("Company Signature Required");
    }

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // Number of outputs is guaranteed as 1
        StockState createdStockState = tx.outputsOfType(StockState.class).get(0);

        requireThat(req -> {
            //Validations when creating a new stock
            req.using("Stock symbol must not be empty", (!createdStockState.getSymbol().isEmpty()));
            req.using("Stock name must not be empty", (!createdStockState.getName().isEmpty()));
            req.using("Stock dividend must start with zero", (!createdStockState.getDividend().equals(BigDecimal.ZERO)));
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // Number of inputs and outputs are guaranteed as 1
        StockState input = tx.inputsOfType(StockState.class).get(0);
        StockState output = tx.outputsOfType(StockState.class).get(0);

        requireThat(req-> {
            //Validations when a stock is updated, ie. AnnounceDividend (UpdateEvolvableToken)
            req.using("Stock Symbol must not be changed.", input.getSymbol().equals(output.getSymbol()));
            req.using("Stock Currency must not be changed.", input.getCurrency().equals(output.getCurrency()));
            req.using("Stock Name must not be changed.", input.getName().equals(output.getName()));
            req.using("Stock Company must not be changed.", input.getIssuer().equals(output.getIssuer()));

            req.using("Stock FractionDigits must not be changed.", input.getFractionDigits() == output.getFractionDigits());
            return null;
        });
    }

}
