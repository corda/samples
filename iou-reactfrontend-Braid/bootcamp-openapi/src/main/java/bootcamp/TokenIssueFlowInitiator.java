package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class TokenIssueFlowInitiator extends FlowLogic<SignedTransaction> {
    private final Party owner;
    private final int amount;

    public TokenIssueFlowInitiator(Party owner, int amount) {
        this.owner = owner;
        this.amount = amount;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Party issuer = getOurIdentity();

        TokenState tokenState = new TokenState(issuer, owner, amount);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        CommandData commandData = new TokenContract.Commands.Issue();

        transactionBuilder.addCommand(commandData, issuer.getOwningKey(), owner.getOwningKey());

        transactionBuilder.addOutputState(tokenState, TokenContract.ID);

        transactionBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(owner);

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
    }
}