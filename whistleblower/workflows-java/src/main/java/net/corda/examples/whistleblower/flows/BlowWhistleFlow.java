package net.corda.examples.whistleblower.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import kotlin.Pair;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.whistleblower.contracts.BlowWhistleContract;
import net.corda.examples.whistleblower.states.BlowWhistleState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * Blows the whistle on a company.
 *
 * Confidential identities are used to preserve the identity of the whistle-blower and the investigator.
 *
 * @param badCompany the company the whistle is being blown on.
 * @param investigator the party handling the investigation.
 */
@InitiatingFlow
@StartableByRPC
public class BlowWhistleFlow extends FlowLogic<SignedTransaction> {
    private static final ProgressTracker.Step GENERATE_CONFIDENTIAL_IDS = new ProgressTracker.Step("Generating confidential identities for the transaction.") {
        @NotNull
        @Override
        public ProgressTracker childProgressTracker() {
            return SwapIdentitiesFlow.tracker();
        }
    };
    private static final ProgressTracker.Step BUILD_TRANSACTION = new ProgressTracker.Step("Building the transaction.");
    private static final ProgressTracker.Step VERIFY_TRANSACTION = new ProgressTracker.Step("Verifying the transaction");
    private static final ProgressTracker.Step SIGN_TRANSACTION = new ProgressTracker.Step("I sign the transaction");
    private static final ProgressTracker.Step COLLECT_COUNTERPARTY_SIG = new ProgressTracker.Step("The counterparty signs the transaction") {
        @NotNull
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.tracker();
        }
    };
    private static final ProgressTracker.Step FINALISE_TRANSACTION = new ProgressTracker.Step("Obtaining notary signatuure and recording transaction") {
        @NotNull
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATE_CONFIDENTIAL_IDS,
            BUILD_TRANSACTION,
            VERIFY_TRANSACTION,
            SIGN_TRANSACTION,
            COLLECT_COUNTERPARTY_SIG,
            FINALISE_TRANSACTION
    );

    private final Party badCompany;
    private final Party investigator;

    public BlowWhistleFlow(Party badCompany, Party investigator) {
        this.badCompany = badCompany;
        this.investigator = investigator;
    }

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(GENERATE_CONFIDENTIAL_IDS);
        FlowSession investigatorSession = initiateFlow(investigator);
        Pair<AnonymousParty, AnonymousParty> confidentialPairMeAndInvestigator = generateConfidentialIdentities(investigatorSession);
        AnonymousParty anonymousMe = confidentialPairMeAndInvestigator.getFirst();
        AnonymousParty anonymousInvestigator = confidentialPairMeAndInvestigator.getSecond();

        progressTracker.setCurrentStep(BUILD_TRANSACTION);
        BlowWhistleState output = new BlowWhistleState(badCompany, anonymousMe, anonymousInvestigator);
        CommandData command = new BlowWhistleContract.Commands.BlowWhistleCmd();
        TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0))
                .addOutputState(output, BlowWhistleContract.ID)
                .addCommand(command, ImmutableList.of(anonymousMe.getOwningKey(), anonymousInvestigator.getOwningKey()));

        progressTracker.setCurrentStep(VERIFY_TRANSACTION);
        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGN_TRANSACTION);
        SignedTransaction stx = getServiceHub().signInitialTransaction(txBuilder, anonymousMe.getOwningKey());

        progressTracker.setCurrentStep(COLLECT_COUNTERPARTY_SIG);
        SignedTransaction ftx = subFlow(new CollectSignaturesFlow(
                stx,
                ImmutableList.of(investigatorSession),
                ImmutableList.of(anonymousMe.getOwningKey()),
                COLLECT_COUNTERPARTY_SIG.childProgressTracker()
        ));

        progressTracker.setCurrentStep(FINALISE_TRANSACTION);
        return subFlow(new FinalityFlow(ftx, ImmutableList.of(investigatorSession), FINALISE_TRANSACTION.childProgressTracker()));
    }

    /** Generates confidential identities for the whistle-blower and the investigator. */
    @Suspendable
    private Pair<AnonymousParty, AnonymousParty> generateConfidentialIdentities(FlowSession counterpartySession) throws FlowException {
        LinkedHashMap<Party, AnonymousParty> confidentialIdentities = subFlow(new SwapIdentitiesFlow(
                counterpartySession,
                GENERATE_CONFIDENTIAL_IDS.childProgressTracker()
        ));
        AnonymousParty anonymousMe = confidentialIdentities.get(getOurIdentity());
        AnonymousParty anonymousInvestigator = confidentialIdentities.get(counterpartySession.getCounterparty());
        return new Pair<AnonymousParty, AnonymousParty>(anonymousMe, anonymousInvestigator);
    }
}
