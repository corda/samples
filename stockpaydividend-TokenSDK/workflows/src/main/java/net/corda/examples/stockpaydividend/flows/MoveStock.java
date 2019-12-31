package net.corda.examples.stockpaydividend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler;
import kotlin.Unit;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.stockpaydividend.flows.utilities.QueryUtilities;
import net.corda.examples.stockpaydividend.states.StockState;

/**
 * Designed initiating node : Company
 * This flow is designed for company to move the issued tokens of stock to the a shareholder node.
 * To make it more real, we can modify it such that the shareholder exchanges some fiat currency for some stock tokens.
 */
@InitiatingFlow
@StartableByRPC
public class MoveStock {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String> {
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
        public String call() throws FlowException {

            // To get the transferring stock, we can get the StockState from the vault and get it's pointer
            TokenPointer<StockState> stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

            // With the pointer, we can get the create an instance of transferring Amount
            Amount<TokenType> amount = new Amount(quantity, stockPointer);

            //Use built-in flow for move tokens to the recipient
            SignedTransaction stx = subFlow(new MoveFungibleTokens(amount, recipient));
            return "\nIssued "+this.quantity +" " +this.symbol+" stocks to "
                    + this.recipient.getName().getOrganisation() + ".\nTransaction ID: "+stx.getId();

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
