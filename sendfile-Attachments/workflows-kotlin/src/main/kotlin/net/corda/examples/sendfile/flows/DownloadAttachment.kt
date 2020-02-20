package net.corda.examples.sendfile.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.examples.sendfile.states.InvoiceState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker
import java.io.File
import java.io.InputStream

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class DownloadAttachment(
        private val sender: Party,
        private val path: String
) : FlowLogic<String>() {
    companion object {
        object RETRIEVING_ID : ProgressTracker.Step("Retrieving attachment ID")
        object DOWNLOAD_ATTACHMENT : ProgressTracker.Step("Download attachment")

        fun tracker() = ProgressTracker(
                RETRIEVING_ID,
                DOWNLOAD_ATTACHMENT
        )
    }

    override val progressTracker = tracker()
    @Suspendable
    override fun call():String {
        progressTracker.currentStep = RETRIEVING_ID
        val criteria = QueryCriteria.VaultQueryCriteria(
                participants = listOf(sender,ourIdentity)
        )

        val state = serviceHub.vaultService.queryBy(
                contractStateType = InvoiceState::class.java,
                criteria = criteria
        ).states.get(0).state.data.invoiceAttachmentID

        progressTracker.currentStep = DOWNLOAD_ATTACHMENT
        val content = serviceHub.attachments.openAttachment(SecureHash.parse(state))!!
        content.open().toFile(path)

        return "Downloaded file from " + sender.name.organisation + " to " + path
    }
}


fun InputStream.toFile(path: String) {
    File(path).outputStream().use { this.copyTo(it) }
}

