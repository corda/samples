package net.corda.examples.sendfile.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Attachment;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.sendfile.states.InvoiceState;
import org.jetbrains.annotations.Nullable;

import java.io.*;

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class DownloadAttachment extends FlowLogic<String> {
    private final Party sender;
    private final String path;

    private final ProgressTracker.Step RETRIEVING_ID = new ProgressTracker.Step("Retrieving attachment ID");
    private final ProgressTracker.Step DOWNLOAD_ATTACHMENT = new ProgressTracker.Step("Download attachment");

    private final ProgressTracker progressTracker = new ProgressTracker(RETRIEVING_ID, DOWNLOAD_ATTACHMENT);

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public DownloadAttachment(Party sender, String path) {
        this.sender = sender;
        this.path = path;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        progressTracker.setCurrentStep(RETRIEVING_ID);
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
                .withParticipants(ImmutableList.of(sender, getOurIdentity()));

        String state = getServiceHub().getVaultService().queryBy(
                InvoiceState.class,
                criteria
        ).getStates().get(0).getState().getData().getInvoiceAttachmentID();

        progressTracker.setCurrentStep(DOWNLOAD_ATTACHMENT);
        Attachment content = getServiceHub().getAttachments().openAttachment(SecureHash.parse(state));
        try {
            assert content != null;
            //content.extractFile(path, new FileOutputStream(new File(path)));
            InputStream inStream = content.open();
            byte[] buffer = new byte[inStream.available()];
            inStream.read(buffer);
            File targetFile = new File(path);
            new FileOutputStream(targetFile).write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Downloaded file from " + sender.getName().getOrganisation() + " to " + path;
    }
}