package net.corda.examples.stockpaydividend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.stockpaydividend.flows.utilities.QueryUtilities;
import net.corda.examples.stockpaydividend.states.StockState;

/**
 * Designed initiating node : Shareholder
 * In real life, shareholder of a stock proactively requests an update.
 * This flow does the same by asking the company to query it's latest transaction of a stock and records it.
 * Note that the shareholder is a participant of the AnnounceDividend flow which therefore ALL_VISIBLE is used.
 *
 * A better approach would be holders triggering this flow every morning.
 */
public class GetStockUpdate {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String>{
        private final String symbol;

        public Initiator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            // Retrieve the most updated and unconsumed StockState and get it's pointer
            // This may be redundant as stock company will query the vault again.
            // But the point is to make sure this node owns this stock as company is considered as an busy node.
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
            StockState stockState = (StockState) stockPointer.getPointer().resolve(getServiceHub()).getState().getData();

            // Send the stock symbol to the company to request for an update.
            FlowSession session = initiateFlow(stockState.getIssuer());
            session.send(stockState.getSymbol());

            // Receive the transaction, checks for the signatures of the state and then record it in vault
            // Note: Instead of ONLY_RELEVANT, ALL_VISIBLE is used here as the shareholder of the StockState is not a participant by the design of this CordApp
            SignedTransaction stx = subFlow(new ReceiveTransactionFlow(session, true, StatesToRecord.ALL_VISIBLE));

            StockState updatedState = getServiceHub().getVaultService().queryBy(StockState.class).getStates().get(0).getState().getData();

            return "\nThe current dividend is: "+ updatedState.getDividend()+ ".";
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{
        private final FlowSession holderSession;

        public Responder(FlowSession holderSession) {
            this.holderSession = holderSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            String symbol = holderSession.receive(String.class).unwrap(it->it);

            // Again, retrieve the most updated and unconsumed StockState and get it's pointer
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

            //Query the most updated stock state
            StateAndRef inputStock = stockPointer.getPointer().resolve(getServiceHub());

            //Retrieve the transaction
            SignedTransaction stx = getServiceHub().getValidatedTransactions().getTransaction(inputStock.getRef().getTxhash());

            //Send it back to the shareholder for update
            subFlow(new SendTransactionFlow(holderSession, stx));

            return null;

        }
    }
}

