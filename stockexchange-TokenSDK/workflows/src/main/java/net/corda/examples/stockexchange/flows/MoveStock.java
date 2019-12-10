package net.corda.examples.stockexchange.flows;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;

import co.paralleluniverse.fibers.Suspendable;
import kotlin.Unit;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.stockexchange.flows.utilities.QueryUtilities;
import net.corda.examples.stockexchange.states.StockState;

@InitiatingFlow
@StartableByRPC
public class MoveStock {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final String symbol;
        private final Long quantity;
        private final Party recipient;

        public Initiator(String symbol, Long quantity, Party recipient) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.recipient = recipient;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // To get the transferring stock, we can get the StockState from the vault and get it's pointer
            TokenPointer<StockState> stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

            // With the pointer, we can get the create an instance of transferring Amount
            Amount<TokenType> amount = new Amount(quantity, stockPointer);

            //Use built-in flow for move tokens to the recipient
            return subFlow(new MoveFungibleTokens(amount, recipient));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Unit>{

        private FlowSession counterSession;

        public Responder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }
}
