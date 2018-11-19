package net.corda.cordaftp


import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * We don't really have a complicated verify with this simple cordapp - it just receives files
 */
@LegalProseReference(uri="/some/uri/docs.txt")
class FileTransferContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        requireThat {
            "No input states (number of inputs is ${tx.inputStates.size})" using (tx.inputStates.isEmpty())
            "One output state (actuals size ${tx.outputStates.size})" using (tx.outputStates.size == 1)
            "Output state is FileTransferManifestState" using (tx.outputStates.single() is FileTransferManifestState)
            "Only two attachments (one data, one contract) (actual size is ${tx.attachments.size})" using (tx.attachments.size == 2)
        }
    }

    // This can go anywhere, but I've put it here for now.
    companion object {
        val FTCONTRACT = "net.corda.cordaftp.FileTransferContract"
    }
}


/**
 * A basic manifest
 */
data class FileTransferManifestState(val sender: Party,
                                     val recipient: Party,
                                     val filename: String,
                                     val senderReference: String,
                                     val recipientReference: String) : ContractState {
    override val participants: List<AbstractParty> get() = listOf(sender, recipient)


    companion object {
        open class FileTransferCommand : TypeOnlyCommandData()
    }
}

/**
 * The flow that the node sending the file initiates
 */
@InitiatingFlow
@StartableByRPC
class TxFileInitiator(private val destinationParty: Party,
                      private val theirReference: String,
                      private val myReference: String,
                      private val file: String,
                      private val attachment: SecureHash,
                      private val postSendAction: PostSendAction?) : FlowLogic<Unit>() {

    companion object {
        object GENERATING : ProgressTracker.Step("Generating")
        object SENDING : ProgressTracker.Step("Sending")
        object POSTSEND : ProgressTracker.Step("Post send actions")
    }

    override val progressTracker = ProgressTracker(GENERATING, SENDING, POSTSEND)

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        progressTracker.currentStep = ProgressTracker.UNSTARTED
        val ptx = TransactionBuilder(notary = notary)
        //val ptx = TransactionType.General.Builder(notary = serviceHub.networkMapCache.getAnyNotary())
        progressTracker.currentStep = GENERATING
        ptx.addAttachment(attachment)
        val me = this.serviceHub.myInfo.legalIdentities.first()
        val outState = FileTransferManifestState(me, destinationParty, file, myReference, theirReference)
        ptx.addOutputState(TransactionState(outState, FileTransferContract.FTCONTRACT, notary))
        val cmd = FileTransferManifestState.Companion.FileTransferCommand()
        ptx.addCommand(cmd, me.owningKey)
        val stx = serviceHub.signInitialTransaction(ptx)
        stx.requiredSigningKeys
        progressTracker.currentStep = SENDING
        val flowSession = initiateFlow(destinationParty)
        subFlow(SendTransactionFlow(flowSession, stx))
        postSendAction?.doAction(file)
        progressTracker.currentStep = POSTSEND

    }
}

/**
 * The platform currently doesn't provide CorDapps a way to access their own config, so we use the CordaService concept
 * to read in our own config file once and store it for use by our flows.
 */
@CordaService
class ConfigHolder(@Suppress("UNUSED_PARAMETER") service: ServiceHub) : SingletonSerializeAsToken() {
    private val destDirs: Map<String, Path>
    init {
        // Look for a file called cordaftp.json in the current working directory (which is usually the node's base dir)
        val configFile = Paths.get("cordaftp.json")
        destDirs = if (Files.exists(configFile)) {
            FileConfigurationReader()
                    .readConfiguration(Files.newInputStream(configFile))
                    .rxMap
                    .values
                    .associateBy({ it.myReference }, { Files.createDirectories(Paths.get(it.destinationDirectory)) })
        } else {
            emptyMap()
        }
    }

    fun getDestDir(reference: String): Path {
        return destDirs[reference] ?: throw IllegalArgumentException("Unknown reference: $reference")
    }
}


/**
 * This flow is started on the receiving node's side when it sees the TxFileInitiator flow send it something
 */
@InitiatedBy(TxFileInitiator::class)
class RxFileResponder(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {
    companion object {
        object RETRIEVING : ProgressTracker.Step("Retrieving")
        object UNPACKING : ProgressTracker.Step("Unpacking")
    }

    override val progressTracker = ProgressTracker(RETRIEVING, UNPACKING)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = RETRIEVING

        val st = subFlow(ReceiveTransactionFlow(otherSideSession, true))

        val state = st.tx.outputs.single().data as FileTransferManifestState

        val attachment = serviceHub.attachments.openAttachment(st.tx.attachments[0])!!

        progressTracker.currentStep = UNPACKING

        // This part ensures that the attachment received is a jar (the Corda spec. requires that this is the case) and
        // then extracts the file to the correct destination directory.

        val configHolder = serviceHub.cordaService(ConfigHolder::class.java)
        attachment.openAsJAR().use { jar ->
            while (true) {
                val nje = jar.nextEntry ?: break
                if (nje.isDirectory) {
                    continue
                }
                val destFile = configHolder.getDestDir(state.recipientReference).resolve(nje.name)
                logger.info("Name is ${nje.name} and path is $destFile")
                Files.newOutputStream(destFile).use {
                    jar.copyTo(it)
                }
            }
        }
    }
}
