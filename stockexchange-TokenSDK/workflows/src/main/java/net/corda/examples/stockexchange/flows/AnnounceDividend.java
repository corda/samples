package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
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
import java.util.Date;
import java.util.List;

/**
 * Designed initiating node : Issuer
 * In this flow, the StockState is updated to declare a number of dividend via the built-in flow UpdateEvolvableToken.
 * The observer then receives a copy of this updated StockState as well.
 * The holder of the tokens of the StockState will not be affected.
 */
public class AnnounceDividend {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String symbol;
        private final BigDecimal dividendQuantity;
        private final Date executionDate;
        private final Date payDate;

        public Initiator(String symbol, BigDecimal dividendQuantity, Date executionDate, Date payDate) {
            this.symbol = symbol;
            this.dividendQuantity = dividendQuantity;
            this.executionDate = executionDate;
            this.payDate = payDate;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Retrieved the unconsumed StockState from the vault
            StateAndRef<StockState> stockStateRef = QueryUtilities.queryStock(symbol, getServiceHub());
            StockState stock = stockStateRef.getState().getData();

            // Form the output state here with a dividend to be announced
            StockState outputState = new StockState(
                    stock.getLinearId(),
                    stock.getIssuer(),
                    stock.getSymbol(),
                    stock.getName(),
                    stock.getCurrency(),
                    dividendQuantity,
                    executionDate,
                    payDate);

            // Get predefined observers
            IdentityService identityService = getServiceHub().getIdentityService();
            List<Party> observers = ObserversUtilities.getObserverLegalIdenties(identityService);

            // Update the stock state and send a copy to the observers eventually
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
            // To implement the responder flow, simply call the subflow of UpdateEvolvableTokenFlowHandler
            return subFlow(new UpdateEvolvableTokenFlowHandler(counterSession));
        }
    }
}
