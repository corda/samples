package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow;
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.stockexchange.flows.utilities.ObserversUtilities;
import net.corda.examples.stockexchange.flows.utilities.QueryUtilities;
import net.corda.examples.stockexchange.states.StockState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * In this flow, the StockState is updated to declare a number of dividend via the built-in flow UpdateEvolvableToken.
 * The holder of the tokens of the StockState will not be affected.
 */
public class AnnounceDividend {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String symbol;
        private final BigDecimal quantity;
        private final Date executionDate;
        private final Date payDate;

        public Initiator(String symbol, BigDecimal quantity, Date executionDate, Date payDate) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.executionDate = executionDate;
            this.payDate = payDate;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Retrieved the unconsumed StockState from the vault
            StateAndRef<StockState> stockStateRef = QueryUtilities.queryStock(symbol, getServiceHub());
            StockState stock = stockStateRef.getState().getData();

            // Form the output state here
            StockState outputState = new StockState(
                    stock.getLinearId(),
                    stock.getMaintainers(),
                    stock.getSymbol(),
                    stock.getName(),
                    stock.getCurrency(),
                    quantity,
                    executionDate,
                    payDate);

            IdentityService identityService = getServiceHub().getIdentityService();

            List<Party> observers = ObserversUtilities.getLegalIdenties(identityService);

            // here use observer approach to send the stock update to exchange and all participants.
            // One of the better design is the participant to request the update before market open by using sendTransactionFlow
            return subFlow(new UpdateEvolvableToken(stockStateRef, outputState, observers));
        }
    }

    @InitiatedBy(AnnounceDividend.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private FlowSession counterSession;

        public Responder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            return subFlow(new UpdateEvolvableTokenFlowHandler(counterSession));
        }
    }
}
