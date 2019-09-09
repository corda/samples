package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import corda.samples.upgrades.contracts.TenderContract;
import corda.samples.upgrades.states.TenderState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

@StartableByRPC
@InitiatingFlow
public class CreateAndPublishTenderFlow extends FlowLogic<SignedTransaction> {

    private String tenderName;
    private Party bidder1;

    public CreateAndPublishTenderFlow(String tenderName, Party bidder1) {
        this.tenderName = tenderName;
        this.bidder1 = bidder1;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party triggeringParty = getOurIdentity();

        TenderState tender = new TenderState(getOurIdentity(), tenderName, 0);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        transactionBuilder.addCommand(new TenderContract.Commands.CreateAndPublish(), ImmutableList.of(triggeringParty.getOwningKey(), bidder1.getOwningKey()));
        transactionBuilder.addOutputState(tender);

        transactionBuilder.verify(getServiceHub());

        final SignedTransaction selfSignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        FlowSession session1 = initiateFlow(bidder1);

        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(selfSignedTx, ImmutableSet.of(session1)));

        return subFlow(new FinalityFlow(signedTransaction, ImmutableSet.of(session1)));
    }
}
