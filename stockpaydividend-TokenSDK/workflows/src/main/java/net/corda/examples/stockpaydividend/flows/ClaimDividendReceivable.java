package net.corda.examples.stockpaydividend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.stockpaydividend.contracts.DividendContract;
import net.corda.examples.stockpaydividend.flows.utilities.QueryUtilities;
import net.corda.examples.stockpaydividend.states.DividendState;
import net.corda.examples.stockpaydividend.states.StockState;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Designed initiating node : Shareholder
 * The shareholder requests the company for issuing a dividend which is to be paid on the payDay.
 * The shareholder first sends a copy of the holding stock.
 * The company builds the transaction which creates a dividend with a reference to the shareholder's stock state
 */
public class ClaimDividendReceivable {

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

            // Retrieve the stock and pointer
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
            StateAndRef<StockState> stockStateRef = stockPointer.getPointer().resolve(getServiceHub());
            StockState stockState = stockStateRef.getState().getData();

            // Query the current Stock amount from shareholder
            Amount<TokenType> stockAmount = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), stockPointer);

            // Prepare to send the stock amount to the company to request dividend issuance
            ClaimNotification stockToClaim = new ClaimNotification(stockAmount);

            FlowSession session = initiateFlow(stockState.getIssuer());

            // First send the stock state as which stock state the shareholder is referring to
            subFlow(new SendStateAndRefFlow(session, ImmutableList.of(stockStateRef)));

            // Then send the stock amount
            session.send(stockToClaim);

            // Wait for the transaction from the company, and sign it after the checking
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(req -> {
                        // Any checkings that the DividendContract is be able to validate. Below are some example constraints
                        DividendState dividend = stx.getTx().outputsOfType(DividendState.class).get(0);
                        req.using("Claimed dividend should be owned by Shareholder", dividend.getHolder().equals(getOurIdentity()));

                        return null;
                    });

                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(session, SignTransactionFlow.Companion.tracker());

            // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
            final SecureHash txId = subFlow(signTxFlow).getId();
            subFlow(new ReceiveFinalityFlow(session, txId));
            return "\nRequest has been sent, Please wait for the stock issuer to respond.";
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

            // Receives shareholder's state for input and output
            List<StateAndRef<StockState>> holderStockStates = subFlow(new ReceiveStateAndRefFlow<>(holderSession));
            StateAndRef<StockState> holderStockState = holderStockStates.get(0);
            StockState stockState = holderStockState.getState().getData();

            // Query the stored state of the company
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(stockState.getSymbol(), getServiceHub());
            StateAndRef<StockState> stockStateRef = stockPointer.getPointer().resolve(getServiceHub());

            // Receives the amount that the shareholder holds
            ClaimNotification claimNoticication = holderSession.receive(ClaimNotification.class).unwrap(it->{
                if(!holderStockState.getRef().getTxhash().equals(stockStateRef.getRef().getTxhash()))
                    throw new FlowException("StockState does not match with the issuers. Shareholder may not have updated the newest stock state.");
                return it;
            });

            PartyAndAmount<TokenType> stockPartyAndAmount = new PartyAndAmount<TokenType>(getOurIdentity(), claimNoticication.getAmount());

            // Preparing the token type of the paying fiat currency
            Currency currency = Currency.getInstance(stockState.getCurrency());
            TokenType dividendTokenType = new TokenType(currency.getCurrencyCode(), currency.getDefaultFractionDigits());

            // Calculate the actual dividend paying to the shareholder
            BigDecimal yield = stockState.getDividend().multiply(BigDecimal.valueOf(claimNoticication.getAmount().getQuantity()));
            BigDecimal dividend = yield.multiply(stockState.getPrice()).multiply(BigDecimal.valueOf(Math.pow(10.0, currency.getDefaultFractionDigits())));

            // Create the dividend state
            Amount<TokenType> dividendAmount = new Amount(dividend.longValue(), dividendTokenType);
            DividendState outputDividend = new DividendState(new UniqueIdentifier(), getOurIdentity(), holderSession.getCounterparty(), new Date(), dividendAmount, false);

            // Start building transaction
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), holderSession.getCounterparty().getOwningKey());
            Command txCommand = new Command(new DividendContract.Commands.Create(), requiredSigners);

            // Using the notary from the previous transaction (dividend issuance)
            Party notary = holderStockState.getState().getNotary();
            TransactionBuilder txBuilder = new TransactionBuilder(notary);

            // Build transaction Add creation of dividend with a reference of the shareholder stock state
            // This  that
            txBuilder
                    .addOutputState(outputDividend, DividendContract.ID)
                    .addReferenceState(new ReferencedStateAndRef(holderStockState))
                    .addCommand(txCommand);

            txBuilder.verify(getServiceHub());

            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());

            final ImmutableSet<FlowSession> sessions = ImmutableSet.of(holderSession);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    sessions));
            return subFlow(new FinalityFlow(stx, sessions));

        }
    }

    @CordaSerializable
    public static class ClaimNotification {
        private Amount<TokenType> amount;

        public ClaimNotification(Amount<TokenType> amount) {
            this.amount = amount;
        }

        public Amount<TokenType> getAmount() {
            return amount;
        }
    }
}

