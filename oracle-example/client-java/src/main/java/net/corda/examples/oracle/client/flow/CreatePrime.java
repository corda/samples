package net.corda.examples.oracle.client.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.oracle.base.contract.PrimeContract;
import net.corda.examples.oracle.base.contract.PrimeState;
import net.corda.examples.oracle.base.flow.QueryPrime;
import net.corda.examples.oracle.base.flow.SignPrime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.util.Collections;
import java.util.function.Predicate;

// The client-side flow that:
// - Uses 'QueryPrime' to request the Nth prime number
// - Adds it to a transaction and signs it
// - Uses 'SignPrime' to gather the oracle's signature attesting that this really is the Nth prime
// - Finalises the transaction
@InitiatingFlow
@StartableByRPC
public class CreatePrime extends FlowLogic<SignedTransaction> {
    private Integer index;

    private static final ProgressTracker.Step SET_UP = new ProgressTracker.Step("Initialising flow.");
    private static final ProgressTracker.Step QUERYING_THE_ORACLE = new ProgressTracker.Step("Querying oracle for the Nth prime.");
    private static final ProgressTracker.Step BUILDING_THE_TX = new ProgressTracker.Step("Building transaction.");
    private static final ProgressTracker.Step VERIFYING_THE_TX = new ProgressTracker.Step("Verifying transaction.");
    private static final ProgressTracker.Step WE_SIGN = new ProgressTracker.Step("signing transaction.");
    private static final ProgressTracker.Step ORACLE_SIGNS = new ProgressTracker.Step("Requesting oracle signature.");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
        @NotNull
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    public ProgressTracker progressTracker = new ProgressTracker(SET_UP, QUERYING_THE_ORACLE, BUILDING_THE_TX, VERIFYING_THE_TX,
                WE_SIGN, ORACLE_SIGNS, FINALISING);

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public CreatePrime(Integer index) {
        this.index = index;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(SET_UP);
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        CordaX500Name oracleName = new CordaX500Name("Oracle", "New York","US");
        Party oracle = getServiceHub().getNetworkMapCache().getNodeByLegalName(oracleName)
                .getLegalIdentities().get(0);
        if (oracle == null) throw new IllegalArgumentException("Requested oracle");

        progressTracker.setCurrentStep(QUERYING_THE_ORACLE);
        Integer nthPrimeRequestedFromOracle = subFlow(new QueryPrime(oracle, index));

        progressTracker.setCurrentStep(BUILDING_THE_TX);
        PrimeState primeState = new PrimeState(index, nthPrimeRequestedFromOracle, getOurIdentity());
        CommandData primeCmdData = new PrimeContract.Commands.Create(index, nthPrimeRequestedFromOracle);
        // By listing the oracle here, we make the oracle a required signer.
        ImmutableList<PublicKey> primeCmdRequiredSigners = ImmutableList.of(oracle.getOwningKey(), getOurIdentity().getOwningKey());

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(primeState, PrimeContract.PRIME_PROGRAM_ID)
                .addCommand(primeCmdData, primeCmdRequiredSigners);

        progressTracker.setCurrentStep(VERIFYING_THE_TX);
        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(WE_SIGN);
        SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(ORACLE_SIGNS);
        // For privacy reasons, we only want to expose to the oracle any commands of type `Prime.Create`
        // that require its signature.
        FilteredTransaction ftx = ptx.buildFilteredTransaction(o -> {
            if (o instanceof Command && ((Command) o).getSigners().contains(oracle.getOwningKey())
                && ((Command) o).getValue() instanceof PrimeContract.Commands.Create) {
                return  true;
            }
            return false;
        });

        TransactionSignature oracleSignature = subFlow(new SignPrime(oracle, ftx));
        SignedTransaction stx = ptx.withAdditionalSignature(oracleSignature);

        progressTracker.setCurrentStep(FINALISING);
        return subFlow(new FinalityFlow(stx, Collections.emptyList()));
    }
}
