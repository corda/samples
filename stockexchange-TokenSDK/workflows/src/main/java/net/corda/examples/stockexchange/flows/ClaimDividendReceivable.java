package net.corda.examples.stockexchange.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.stockexchange.contracts.DividendContract;
import net.corda.examples.stockexchange.flows.utilities.QueryUtilities;
import net.corda.examples.stockexchange.flows.utilities.TempTokenSelectionFactory;
import net.corda.examples.stockexchange.states.DividendState;
import net.corda.examples.stockexchange.states.StockState;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ClaimDividendReceivable {

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

            // Retrieve the stock and pointer
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
            StateAndRef<StockState> stockStateRef = stockPointer.getPointer().resolve(getServiceHub());
            StockState stockState = stockStateRef.getState().getData();

            // Query the current Stock amount from shareholder
            Amount<TokenType> stockAmount = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), stockPointer);

            // Prepare to send the stock amount to the issuer to request dividend issuance
            ClaimNotification stockToClaim = new ClaimNotification(stockAmount);

            FlowSession session = initiateFlow(stockState.getIssuer());

            // First send the stock state as which stock state the shareholder is referring to
            subFlow(new SendStateAndRefFlow(session, ImmutableList.of(stockStateRef)));

            // Then send the stock amount
            session.send(stockToClaim);

            // Wait for the transaction from the issuer, and sign it after the checking
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(req -> {
                        // TODO Add more constraints on the shareholder side

                        // Example constraints

                        if (stx.getTx().getCommands().stream().noneMatch(c->c.getValue() instanceof DividendContract.Commands.Create)){
                            throw new IllegalArgumentException("Invalid Command. Expecting: DividendContract.Commands.Create");
                        }

                        List<DividendState> outputDividends = stx.getTx().outputsOfType(DividendState.class);
                        req.using("There must be one output dividend.", outputDividends.size() == 1);

                        DividendState dividend = outputDividends.get(0);
                        req.using("Claimed dividend should be owned by Shareholder", dividend.getHolder().equals(getOurIdentity()));

                        return null;
                    });

                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(session, SignTransactionFlow.Companion.tracker());

            // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
            final SecureHash txId = subFlow(signTxFlow).getId();
            return subFlow(new ReceiveFinalityFlow(session, txId));
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

            // Receives holder's state for input and output
            List<StateAndRef<StockState>> holderStockStates = subFlow(new ReceiveStateAndRefFlow<>(holderSession));
            StateAndRef<StockState> holderStockState = holderStockStates.get(0);
            StockState stockState = holderStockState.getState().getData();

            // Query the stored state of the issuer
            TokenPointer stockPointer = QueryUtilities.queryStockPointer(stockState.getSymbol(), getServiceHub());
            StateAndRef<StockState> stockStateRef = stockPointer.getPointer().resolve(getServiceHub());

            // Receives the amount that the holder holds
            ClaimNotification claimNoticication = holderSession.receive(ClaimNotification.class).unwrap(it->{
                if(!holderStockState.getRef().getTxhash().equals(stockStateRef.getRef().getTxhash()))
                    throw new FlowException("StockState does not match with the issuers. Shareholder may not have updated the newest stock state.");
                return it;
            });

            PartyAndAmount<TokenType> stockPartyAndAmount = new PartyAndAmount<TokenType>(getOurIdentity(), claimNoticication.getAmount());

            // Calculate the dividend state
            BigDecimal dividend = stockState.getDividend().multiply(BigDecimal.valueOf(claimNoticication.getAmount().getQuantity()));

            // Create the dividend state
            Currency currency = Currency.getInstance(stockState.getCurrency());
            TokenType dividendTokenType = new TokenType(currency.getCurrencyCode(), currency.getDefaultFractionDigits());
            Amount<TokenType> dividendAmount = new Amount(dividend.longValue(), dividendTokenType);
            DividendState outputDividend = new DividendState(new UniqueIdentifier(), getOurIdentity(), holderSession.getCounterparty(), new Date(), dividendAmount, false);

            // Start building transaction
            List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), holderSession.getCounterparty().getOwningKey());
            Command txCommand = new Command(new DividendContract.Commands.Create(), requiredSigners);

            // Using the notary from the previous transaction (dividend issuance)
            Party notary = holderStockState.getState().getNotary();
            TransactionBuilder txBuilder = new TransactionBuilder(notary);

            // Build transaction 1/2 - Add the received StockState as input and output with no change
            TokenSelection tokenSelector = TempTokenSelectionFactory.getTokenSelection(getServiceHub());

            // Generate move of the stocks with better performance handling. See Corda/token-sdk/token-selection.md for more details
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> stockIoPair = tokenSelector.generateMove(getRunId().getUuid(), ImmutableList.of(stockPartyAndAmount), getOurIdentity(), null);

            // Using TokenSDK utilities to help building the transaction
            MoveTokensUtilitiesKt.addMoveTokens(txBuilder, stockIoPair.getFirst(), stockIoPair.getSecond());

            // Build transaction 2/2 - Add creation of dividend
            txBuilder
                    .addOutputState(outputDividend, DividendContract.ID)
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

