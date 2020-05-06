package net.corda.examples.stockpaydividend.flows.utilities;

import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.ServiceHub;
import net.corda.examples.stockpaydividend.states.StockState;

import java.util.List;

public class QueryUtilities {

    /**
     * Retrieve any unconsumed StockState and filter by the given symbol
     */
    public static StateAndRef<StockState> queryStock(String symbol, ServiceHub serviceHub){
        List<StateAndRef<StockState>> stateAndRefs = serviceHub.getVaultService().queryBy(StockState.class).getStates();

        // Match the query result with the symbol. If no results match, throw exception
        StateAndRef<StockState> stockStateAndRef = stateAndRefs.stream()
                .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+symbol+"\" not found from vault"));

        return stockStateAndRef;
    }

    /**
     * Retrieve any unconsumed StockState and filter by the given symbol
     * Then return the pointer to this StockState
     */
    public static TokenPointer<StockState> queryStockPointer(String symbol, ServiceHub serviceHub){
        StateAndRef<StockState> stockStateStateAndRef = queryStock(symbol, serviceHub);
        return stockStateStateAndRef.getState().getData().toPointer(StockState.class);
    }
}
