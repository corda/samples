package net.corda.examples.attachments.contract

import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractAttachment
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction
import net.corda.examples.attachments.state.AgreementState

open class AgreementContract : Contract {
    companion object {
        const val AGREEMENT_CONTRACT_ID = "net.corda.examples.attachments.contract.AgreementContract"
        val BLACKLIST_JAR_HASH = SecureHash.parse("4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A")
    }

    override fun verify(tx: LedgerTransaction) = requireThat {
        // Constraints on the inputs, outputs and commands.
        "The transaction should have no inputs" using (tx.inputs.isEmpty())
        "The transaction should have an AgreementState output" using (tx.outputsOfType<AgreementState>().size == 1)
        "The transaction should have no other outputs" using (tx.outputs.size == 1)
        "The transaction should have an Agree command" using (tx.commandsOfType<Commands.Agree>().size == 1)
        "The transaction should have no other commands" using (tx.commands.size == 1)

        // Constraints on the included attachments.
        val nonContractAttachments = tx.attachments.filter { it !is ContractAttachment }
        "The transaction should have a single non-contract attachment" using (nonContractAttachments.size == 1)
        val attachment = nonContractAttachments.single()

        // TODO: Switch to constraint on the jar's signer.
        // In the future, Corda will support the signing of jars. We will then be able to restrict
        // the attachments used to just those signed by party X.
        "The jar's hash should be correct" using (attachment.id == BLACKLIST_JAR_HASH)

        // We extract the blacklisted company names from the JAR.
        val attachmentJar = attachment.openAsJAR()
        while (attachmentJar.nextEntry.name != "blacklist.txt") {
            // Calling `attachmentJar.nextEntry` causes us to scroll through the JAR.
        }
        val blacklistedCompanies = mutableListOf<String>()
        val bufferedReader = attachmentJar.bufferedReader()
        var company = bufferedReader.readLine()
        while (company != null) {
            blacklistedCompanies.add(company)
            company = bufferedReader.readLine()
        }

        // Constraints on the blacklisted parties.
        val agreement = tx.outputsOfType<AgreementState>().single()
        val participants = agreement.participants
        val participantsOrgs = participants.map { it.name.organisation }
        val overlap = blacklistedCompanies.toSet().intersect(participantsOrgs)
        "The agreement involved blacklisted parties: $overlap" using (overlap.isEmpty())

        // Constraints on the signers.
        val command = tx.commands.single()
        val participantKeys = participants.map { it.owningKey }
        "All the parties to the agreement are signers" using (command.signers.containsAll(participantKeys))
    }

    interface Commands {
        class Agree : TypeOnlyCommandData(), Commands
    }
}
