package net.corda.examples.stockpaydividend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.stockpaydividend.contracts.DividendContract;
import net.corda.examples.stockpaydividend.flows.utilities.TempTokenSelectionFactory;
import net.corda.examples.stockpaydividend.states.DividendState;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Designed initiating node : Company
 * Company pays off any dividend that it should be paid.
 * Should also look at how TokenSelection.generateMove() and MoveTokensUtilitiesKt.addMoveTokens() work together to simply create a transfer of tokens
 */
public class PayDividend {
    private final ProgressTracker progressTracker = new ProgressTracker();

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<List<String>> {

        @Override
        @Suspendable
        public List<String> call() throws FlowException {

            //Query the vault for any unconsumed DividendState
            List<StateAndRef<DividendState>> stateAndRefs = getServiceHub().getVaultService().queryBy(DividendState.class).getStates();

            List<SignedTransaction> transactions = new ArrayList<>();
            List<String> notes = new ArrayList<>();

            //For each queried unpaid DividendState, pay off the dividend with the corresponding amount.
            for(StateAndRef<DividendState> result : stateAndRefs){
                DividendState dividendState =  result.getState().getData();
                Party shareholder = dividendState.getHolder();

                // The amount of fiat tokens to be sent to the shareholder.
                PartyAndAmount<TokenType> sendingPartyAndAmount = new PartyAndAmount<>(shareholder, dividendState.getDividendAmount());

                // Instantiating an instance of TokenSelection which helps retrieving required tokens easily
                TokenSelection tokenSelection = TempTokenSelectionFactory.getTokenSelection(getServiceHub());

                // Generate input and output pair of moving fungible tokens
                Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> fiatIoPair = tokenSelection.generateMove(
                                getRunId().getUuid(),
                                ImmutableList.of(sendingPartyAndAmount),
                                getOurIdentity(),
                                null);

                // Using the notary from the previous transaction (dividend issuance)
                Party notary = result.getState().getNotary();

                // Create the required signers and the command
                List<PublicKey> requiredSigners = dividendState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
                Command payCommand = new Command(new DividendContract.Commands.Pay(), requiredSigners);

                // Start building transaction
                TransactionBuilder txBuilder = new TransactionBuilder(notary);
                txBuilder
                        .addInputState(result)
                        .addCommand(payCommand);
                // As a later part of TokenSelection.generateMove which generates a move of tokens handily
                MoveTokensUtilitiesKt.addMoveTokens(txBuilder, fiatIoPair.getFirst(), fiatIoPair.getSecond());

                // Verify the transactions with contracts
                txBuilder.verify(getServiceHub());

                // Sign the transaction
                SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());

                // Instantiate a network session with the shareholder
                FlowSession holderSession = initiateFlow(shareholder);

                final ImmutableSet<FlowSession> sessions = ImmutableSet.of(holderSession);

                // Ask the shareholder to sign the transaction
                final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                        ptx,
                        ImmutableSet.of(holderSession)));
                SignedTransaction fstx = subFlow(new FinalityFlow(stx, sessions));
                notes.add("\nPaid to " + dividendState.getHolder().getName().getOrganisation()
                        + " " + (dividendState.getDividendAmount().getQuantity()/100) +" "
                        + dividendState.getDividendAmount().getToken().getTokenIdentifier() + "\nTransaction ID: " + fstx.getId() );
            }
            return notes;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{
        private final FlowSession session;

        public Responder(FlowSession session) {
            this.session = session;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Override the SignTransactionFlow for custom checkings
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(req -> {
                        // Any checkings that the DividendContract is be not able to validate.
                        List<FungibleToken> outputFiats = stx.getTx().outputsOfType(FungibleToken.class);
                        List<FungibleToken> holderFiats = outputFiats.stream().filter(fiat->fiat.getHolder().equals(getOurIdentity())).collect(Collectors.toList());;
                        req.using("One FungibleToken output should be held by Shareholder", holderFiats.size()==1);
                        return null;
                    });

                }
            }
            // Wait for the transaction from the company, and sign it after the checking
            final SignTxFlow signTxFlow = new SignTxFlow(session, SignTransactionFlow.Companion.tracker());

            // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
            final SecureHash txId = subFlow(signTxFlow).getId();
            return subFlow(new ReceiveFinalityFlow(session, txId));

        }
    }
}

