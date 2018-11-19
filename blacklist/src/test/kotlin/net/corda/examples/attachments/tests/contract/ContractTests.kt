package net.corda.examples.attachments.tests.contract

import net.corda.core.crypto.generateKeyPair
import net.corda.core.identity.CordaX500Name
import net.corda.examples.attachments.BLACKLIST_JAR_PATH
import net.corda.examples.attachments.contract.AgreementContract
import net.corda.examples.attachments.contract.AgreementContract.Companion.AGREEMENT_CONTRACT_ID
import net.corda.examples.attachments.state.AgreementState
import net.corda.examples.attachments.tests.BLACKLISTED_PARTIES
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.getTestPartyAndCertificate
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import java.io.File

class ContractTests {
    private val ledgerServices = MockServices(listOf("net.corda.examples.attachments.contract"), identityService = makeTestIdentityService(), initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")))
    private val megaCorpName = CordaX500Name("MegaCorp", "London", "GB")
    private val miniCorpName = CordaX500Name("MiniCorp", "London", "GB")
    private val megaCorpIdentity = TestIdentity(megaCorpName)
    private val miniCorpIdentity = TestIdentity(miniCorpName)
    private val megaCorp = megaCorpIdentity.party
    private val miniCorp = miniCorpIdentity.party
    private val megaCorpPubKey = megaCorpIdentity.publicKey
    private val miniCorpPubKey = miniCorpIdentity.publicKey

    private val agreementTxt = "$megaCorpName agrees with $miniCorpName that..."
    private val validAttachment = File(BLACKLIST_JAR_PATH)
    private val blacklistedPartyKeyPair = generateKeyPair()
    private val blacklistedPartyPubKey = blacklistedPartyKeyPair.public
    private val blacklistedPartyName = CordaX500Name(organisation = BLACKLISTED_PARTIES[0], locality = "London", country = "GB")
    private val blacklistedParty = getTestPartyAndCertificate(blacklistedPartyName, blacklistedPartyPubKey).party

    @Test
    fun `agreement transaction contains one non-contract attachment`() {
        ledgerServices.ledger {
            // We upload a test attachment to the ledger.
            val attachmentInputStream = validAttachment.inputStream()
            val attachmentHash = attachment(attachmentInputStream)
            println(attachmentHash)

            transaction {
                output(AGREEMENT_CONTRACT_ID, AgreementState(megaCorp, miniCorp, agreementTxt))
                command(listOf(megaCorpPubKey, miniCorpPubKey), AgreementContract.Commands.Agree())
                fails()
                attachment(attachmentHash)
                verifies()
            }
        }
    }

    @Test
    fun `the non-contract attachment must not blacklist any of the participants`() {
        ledgerServices.ledger {
            // We upload a test attachment to the ledger.
            val attachmentInputStream = validAttachment.inputStream()
            val attachmentHash = attachment(attachmentInputStream)

            transaction {
                output(AGREEMENT_CONTRACT_ID, AgreementState(megaCorp, blacklistedParty, agreementTxt))
                command(listOf(megaCorpPubKey, blacklistedPartyPubKey), AgreementContract.Commands.Agree())
                attachment(attachmentHash)
                fails()
            }
        }
    }
}