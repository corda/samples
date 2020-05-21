package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.OldContract;
import corda.samples.upgrades.states.OldState;
import net.corda.core.contracts.SignatureAttachmentConstraint;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class ImplicitMigrateToSignatureConstraintFlow extends FlowLogic<SignedTransaction> {

    private Party counterParty;
    private int amount;

    public ImplicitMigrateToSignatureConstraintFlow(Party counterParty, int amount) {
        this.counterParty = counterParty;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {


        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        StateAndRef<OldState> input = getServiceHub().getVaultService()
                .queryBy(OldState.class,
                new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).getStates().get(0);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        OldState output = new OldState(getOurIdentity() , counterParty , amount);

        transactionBuilder.addInputState(input);

        /* you can explicitly specify signature constraint
        SecureHash attachment = this.getServiceHub().getCordappProvider().getContractAttachmentID("corda.samples.upgrades.contracts.OldContract");

        List<PublicKey> signers = getServiceHub().getAttachments().openAttachment(attachment).getSignerKeys();

        // Create the key that will have to pass for all future versions.
        PublicKey ownersKey = signers.get(0);

        transactionBuilder.addOutputState(output , "corda.samples.upgrades.contracts.OldContract" , new SignatureAttachmentConstraint(ownersKey));

        transactionBuilder.addOutputState(output, new SignatureAttachmentConstraint(ownersKey));
        */

        transactionBuilder.addOutputState(output);

        transactionBuilder.addCommand(new OldContract.Commands.Issue() ,
                ImmutableList.of(getOurIdentity().getOwningKey() , counterParty.getOwningKey()));

        transactionBuilder.verify(getServiceHub());

        SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        FlowSession flowSession = initiateFlow(counterParty);

        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, ImmutableList.of(flowSession)));

        return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(flowSession)));

    }
}
