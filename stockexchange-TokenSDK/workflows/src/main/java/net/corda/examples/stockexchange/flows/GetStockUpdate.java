package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.stockexchange.flows.utilities.QueryUtilities;
import net.corda.examples.stockexchange.states.StockState;

public class GetStockUpdate {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private final String symbol;

        public Initiator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

            StockState stockState = (StockState) stockPointer.getPointer().resolve(getServiceHub()).getState().getData();

            // Send symbol to the query
            FlowSession session = initiateFlow(stockState.getIssuer());
            session.send(stockState.getSymbol());

            // Receive the transaction, checks for the signatures of the state and then record it in vault
            return subFlow(new ReceiveTransactionFlow(session, true, StatesToRecord.ONLY_RELEVANT));
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

            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

            //Query the most updated stock state
            StateAndRef inputStock = stockPointer.getPointer().resolve(getServiceHub());

            //Retrieve the transaction
            SignedTransaction stx =getServiceHub().getValidatedTransactions().getTransaction(inputStock.getRef().getTxhash());

            //Send it back to the shareholder for update
            subFlow(new SendTransactionFlow(holderSession, stx));

            return null;

        }
    }
}

