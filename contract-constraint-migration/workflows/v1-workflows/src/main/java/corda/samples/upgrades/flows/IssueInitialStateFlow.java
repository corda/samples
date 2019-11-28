package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.OldContract;
import corda.samples.upgrades.states.OldState;
import net.corda.core.contracts.HashAttachmentConstraint;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

@InitiatingFlow
@StartableByRPC
public class IssueInitialStateFlow extends FlowLogic<SignedTransaction> {

    private Party counterParty;
    private int amount;

    public IssueInitialStateFlow(Party counterParty, int amount) {
        this.counterParty = counterParty;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Party issuer = getOurIdentity();

        OldState outputState = new OldState(issuer, counterParty, amount);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        /* Explicit way of specifying constraint to override the default behavior.
        SecureHash secureHash = SecureHash.parse("56c51e6d4f3553f70e39af00b469e8fdabd7b17876107a4177e8a00b1a33ef86");
        transactionBuilder.addOutputState(outputState, new HashAttachmentConstraint(secureHash));
        */

        transactionBuilder.addOutputState(outputState);

        transactionBuilder.addCommand(new OldContract.Commands.Issue() ,
                ImmutableList.of(issuer.getOwningKey() , counterParty.getOwningKey()));

        transactionBuilder.verify(getServiceHub());

        SignedTransaction partialSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        FlowSession flowSession = initiateFlow(counterParty);

        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partialSignedTransaction, ImmutableList.of(flowSession)));

        return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(flowSession)));
    }
}
