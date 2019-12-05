package net.corda.examples.stockexchange.flows;

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
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.stockexchange.contracts.DividendContract;
import net.corda.examples.stockexchange.flows.utilities.TempTokenSelectionFactory;
import net.corda.examples.stockexchange.states.DividendState;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class PayDividend {
    private final ProgressTracker progressTracker = new ProgressTracker();

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<List<SignedTransaction>> {

        @Override
        @Suspendable
        public List<SignedTransaction> call() throws FlowException {

            QueryCriteria.VaultQueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            Vault.Page<DividendState> results = getServiceHub().getVaultService().queryBy(DividendState.class, generalCriteria);

            List<SignedTransaction> transactions = new ArrayList<>();

            for(StateAndRef<DividendState> result : results.getStates()){
                DividendState dividendState =  result.getState().getData();
                Party holder = dividendState.getHolder();

                // Prepare fiat
                TokenSelection tokenSelection = TempTokenSelectionFactory.getTokenSelection(getServiceHub());
                PartyAndAmount<TokenType> sendingPartyAndAmount = new PartyAndAmount<>(holder, dividendState.getDividendAmount());

                // Generate input and output pair of moving fungible tokens
                Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> fiatIoPair = tokenSelection.generateMove(
                                getRunId().getUuid(),
                                ImmutableList.of(sendingPartyAndAmount),
                                getOurIdentity(),
                                null);

                // Using the notary from the previous transaction (dividend issuance)
                Party notary = result.getState().getNotary();

                List<PublicKey> requiredSigners = dividendState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
                Command payCommand = new Command(new DividendContract.Commands.Pay(), requiredSigners);

                // Start building transaction
                TransactionBuilder txBuilder = new TransactionBuilder(notary);
                txBuilder
                        .addInputState(result)
                        .addCommand(payCommand);
                MoveTokensUtilitiesKt.addMoveTokens(txBuilder, fiatIoPair.getFirst(), fiatIoPair.getSecond());

                txBuilder.verify(getServiceHub());

                SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());

                FlowSession holderSession = initiateFlow(holder);

                final ImmutableSet<FlowSession> sessions = ImmutableSet.of(holderSession);
                final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                        ptx,
                        sessions));
                transactions.add(subFlow(new FinalityFlow(stx, sessions)));
            }
            return transactions;
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
                        // TODO Add more constraints on the shareholder side

                        // Example constraints

                        if (stx.getTx().getCommands().stream().noneMatch(c->c.getValue() instanceof DividendContract.Commands.Pay)){
                            throw new IllegalArgumentException("Invalid Command. Expecting: DividendContract.Commands.Pay");
                        }

                        List<FungibleToken> outputFiats = stx.getTx().outputsOfType(FungibleToken.class);
                        List<FungibleToken> holderFiats = outputFiats.stream().filter(fiat->fiat.getHolder().equals(getOurIdentity())).collect(Collectors.toList());;
                        req.using("One FungibleToken output should be held by Shareholder", holderFiats.size()==1);

                        return null;
                    });

                }
            }
            // Wait for the transaction from the issuer, and sign it after the checking
            final SignTxFlow signTxFlow = new SignTxFlow(session, SignTransactionFlow.Companion.tracker());

            // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
            final SecureHash txId = subFlow(signTxFlow).getId();
            return subFlow(new ReceiveFinalityFlow(session, txId));

        }
    }
}

