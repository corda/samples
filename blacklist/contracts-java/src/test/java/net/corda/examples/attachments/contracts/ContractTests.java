package net.corda.examples.attachments.contracts;

import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.examples.attachments.states.AgreementState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyPair;
import java.security.PublicKey;

import static java.util.Arrays.asList;
import static net.corda.core.crypto.CryptoUtils.generateKeyPair;
import static net.corda.examples.attachments.Constants.BLACKLISTED_PARTIES;
import static net.corda.examples.attachments.Constants.BLACKLIST_JAR_PATH;
import static net.corda.examples.attachments.contracts.AgreementContract.AGREEMENT_CONTRACT_ID;
import static net.corda.testing.core.TestUtils.getTestPartyAndCertificate;
import static net.corda.testing.node.MockServicesKt.makeTestIdentityService;
import static net.corda.testing.node.NodeTestUtils.ledger;


public class ContractTests {
    static private final MockServices ledgerServices = new MockServices(asList("net.corda.examples.attachments.contracts"),  new TestIdentity(new CordaX500Name("TestIdentity", "", "GB")), makeTestIdentityService());
    static private final CordaX500Name megaCorpName = new CordaX500Name("MegaCorp", "London", "GB");
    static private final CordaX500Name miniCorpName = new CordaX500Name("MiniCorp", "London", "GB");
    static private final TestIdentity megaCorp = new TestIdentity(megaCorpName);
    static private final TestIdentity miniCorp = new TestIdentity(miniCorpName);

    static private final String agreementTxt = megaCorpName + " agrees with " + miniCorpName + " that...";
    static private final File validAttachment = new File(BLACKLIST_JAR_PATH);
    static private final KeyPair blacklistedPartyKeyPair = generateKeyPair();
    static private final PublicKey blacklistedPartyPubKey = blacklistedPartyKeyPair.getPublic();
    static private final CordaX500Name blacklistedPartyName = new CordaX500Name(BLACKLISTED_PARTIES.get(0), "London", "GB");
    static private final Party blacklistedParty = getTestPartyAndCertificate(blacklistedPartyName, blacklistedPartyPubKey).getParty();

    @Test
    public void agreementTransactionContainsOneNonContractAttachment() {
        ledger(ledgerServices, (ledger -> {
            // We upload a test attachment to the ledger.
            FileInputStream attachmentInputStream = null;
            try {
                attachmentInputStream = new FileInputStream(validAttachment);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            SecureHash attachmentHash = ledger.attachment(attachmentInputStream);

            ledger.transaction(tx -> {
                tx.output(AGREEMENT_CONTRACT_ID, new AgreementState(megaCorp.getParty(), miniCorp.getParty(), agreementTxt));
                tx.command(asList(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new AgreementContract.Commands.Agree());
                tx.fails();
                tx.attachment(attachmentHash);
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void nonContractAttachmentMustNotBlacklistAnyOfTheParticipants() {
        ledger(ledgerServices, (ledger -> {
            // We upload a test attachment to the ledger.
            FileInputStream attachmentInputStream = null;
            try {
                attachmentInputStream = new FileInputStream(validAttachment);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            SecureHash attachmentHash = ledger.attachment(attachmentInputStream);

            ledger.transaction(tx -> {
                tx.output(AGREEMENT_CONTRACT_ID, new AgreementState(megaCorp.getParty(), blacklistedParty, agreementTxt));
                tx.command(asList(megaCorp.getPublicKey(), blacklistedPartyPubKey), new AgreementContract.Commands.Agree());
                tx.attachment(attachmentHash);
                tx.fails();

                return null;
            });

            ledger.transaction(tx -> {
                tx.output(AGREEMENT_CONTRACT_ID, new AgreementState(megaCorp.getParty(), miniCorp.getParty(), agreementTxt));
                tx.command(asList(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new AgreementContract.Commands.Agree());
                tx.attachment(attachmentHash);
                tx.verifies();

                return null;
            });


            return null;
        }));
    }
}
