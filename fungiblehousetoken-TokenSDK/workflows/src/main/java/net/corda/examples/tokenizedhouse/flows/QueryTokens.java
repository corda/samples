package net.corda.examples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.examples.tokenizedhouse.states.FungibleHouseTokenState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;

public class QueryTokens {

    @InitiatingFlow
    @StartableByRPC
    public static class GetTokenBalance extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String symbol;


        public GetTokenBalance(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleHouseTokenState evolvableTokenType = stateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer<FungibleHouseTokenState> tokenPointer = evolvableTokenType.toPointer(FungibleHouseTokenState.class);

            Amount<TokenType> amount = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), tokenPointer);
            return "\n You currently have "+ amount.getQuantity()+ " " +symbol + " Tokens\n";
        }
    }

}