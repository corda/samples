package net.corda.examples.stockexchange.flows;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow;
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

            TokenPointer<StockState> stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

            Amount<TokenType> amount = new Amount<TokenType>(quantity, stockPointer);

            PartyAndAmount<TokenType> partyAndAmount = new PartyAndAmount<TokenType>(recipient, amount);

            FlowSession session = initiateFlow(recipient);

            //use built in flow for issuing tokens on ledger
            return subFlow(new MoveFungibleTokensFlow(ImmutableList.of(partyAndAmount), ImmutableList.of(session)));
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
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }
}
