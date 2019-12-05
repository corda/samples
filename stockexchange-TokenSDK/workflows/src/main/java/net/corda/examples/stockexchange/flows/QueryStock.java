package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.utilities.FlowUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.stockexchange.flows.utilities.QueryUtilities;
import org.slf4j.Logger;

public class QueryStock {

    @StartableByRPC
    @InitiatingFlow
    public class QueryTokenBalance extends FlowLogic<Amount<TokenPointer>> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String symbol;

        public QueryTokenBalance(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public Amount<TokenPointer> call() throws FlowException {
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);

            Amount<TokenPointer> amount = null;

            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
            if(stockPointer != null)
                amount = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), stockPointer);

            return amount;
        }
    }

    @StartableByRPC
    @InitiatingFlow
    public class QueryStockDetail extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String symbol;

        public QueryStockDetail(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String call() throws FlowException {
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
            StateAndRef stockRefAndState = stockPointer.getPointer().resolve(getServiceHub());
            return stockRefAndState.getState().getData().toString();
        }
    }

//    @StartableByRPC
//    @InitiatingFlow
//    public class GetTransaction extends FlowLogic<String> {
//        private final ProgressTracker progressTracker = new ProgressTracker();
//
//        @Override
//        public String call() throws FlowException {
//            StringBuilder msg = new StringBuilder("******************\n");
//            Logger logger = getLogger();
//            ServiceHub serviceHub = getServiceHub();
//
//            for (SignedTransaction item : getServiceHub().getValidatedTransactions().track().getSnapshot()){
//                WireTransaction tx = item.getTx();
//
//                logger.info("*****************");
//                logger.info("Txn hash: " + item.getTx().toLedgerTransaction(serviceHub).hashCode());
//                if (tx.toLedgerTransaction(serviceHub).getInputs().size() >0) {
//                    logger.info("***input class - " +tx.toLedgerTransaction(serviceHub).getInput(0).getClass().toString());
//                    logger.info("***input state - " +tx.toLedgerTransaction(serviceHub).getInput(0).toString());
//                    msg.append("input: ");
//                    msg.append(tx.toLedgerTransaction(serviceHub).getInput(0).toString());
//                    msg.append("\n");
//                }
//                if (tx.getOutputs().size() >0) {
//                    logger.info("***output class - " +tx.getOutput(0).getClass().toString());
//                    logger.info("***output state -" + tx.getOutput(0).toString());
//                    msg.append("output: ");
//                    msg.append(tx.toLedgerTransaction(serviceHub).getOutput(0).toString());
//                    msg.append("\n");
//                }
//                for (AbstractParty p : FlowUtilitiesKt.getParticipants(tx.toLedgerTransaction(serviceHub))){
//                    logger.info("Participant:" + serviceHub.getIdentityService().partyFromKey(p.getOwningKey()).getName());
//                    msg.append("participants: ");
//                    msg.append(serviceHub.getIdentityService().partyFromKey(p.getOwningKey()).getName());
//                    msg.append("\n");
//                }
//                logger.info("*****************");
//
//            }
//            return msg.toString();
//        }
//    }
}

