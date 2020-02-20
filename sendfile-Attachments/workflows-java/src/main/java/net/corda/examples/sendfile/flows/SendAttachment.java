package net.corda.examples.sendfile.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.sendfile.contracts.InvoiceContract;
import net.corda.examples.sendfile.states.InvoiceState;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class SendAttachment extends FlowLogic<SignedTransaction> {
    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction");
    private final ProgressTracker.Step PROCESSING_TRANSACTION = new ProgressTracker.Step("PROCESS transaction");
    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.");

    private final ProgressTracker progressTracker =
            new ProgressTracker(GENERATING_TRANSACTION, PROCESSING_TRANSACTION, FINALISING_TRANSACTION);

    private final Party receiver;

    public SendAttachment(Party receiver) {
        this.receiver = receiver;
    }

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // initiate notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Initiate transaction Builder
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        // upload attachment via private method
        String path = System.getProperty("user.dir");
        System.out.println("Working Directory = " + path);

        //Change the path to "../test.zip" for passing the unit test.
        //because the unit test are in a different working directory than the running node.
        SecureHash attachmentHash = null;
        try {
            attachmentHash = SecureHash.parse(uploadAttachment(
                    "../../../../test.zip",
                    getServiceHub(),
                    getOurIdentity(),
                    "testzip")
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        // build transaction
        InvoiceState output = new InvoiceState(attachmentHash.toString(), ImmutableList.of(getOurIdentity(), receiver));
        InvoiceContract.Commands.Issue commandData = new InvoiceContract.Commands.Issue();
        transactionBuilder.addCommand(commandData, getOurIdentity().getOwningKey(), receiver.getOwningKey());
        transactionBuilder.addOutputState(output, InvoiceContract.ID);
        transactionBuilder.addAttachment(attachmentHash);
        transactionBuilder.verify(getServiceHub());

        // self signing
        progressTracker.setCurrentStep(PROCESSING_TRANSACTION);
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        // counter parties signing
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);

        FlowSession session = initiateFlow(receiver);
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, ImmutableList.of(session)));

        return subFlow(new FinalityFlow(fullySignedTransaction, ImmutableList.of(session)));
    }

    private String uploadAttachment(String path, ServiceHub service, Party whoami, String filename) throws IOException {
        SecureHash attachmentHash = service.getAttachments().importAttachment(
                new FileInputStream(new File(path)),
                whoami.toString(),
                filename
        );

        return attachmentHash.toString();
    }
}
