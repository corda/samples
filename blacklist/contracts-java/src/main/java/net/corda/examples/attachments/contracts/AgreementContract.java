package net.corda.examples.attachments.contracts;

import kotlin.text.Charsets;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.attachments.states.AgreementState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class AgreementContract implements Contract {
    public static final String AGREEMENT_CONTRACT_ID = "net.corda.examples.attachments.contracts.AgreementContract";
    private static SecureHash BLACKLIST_JAR_HASH = SecureHash.parse("4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A");

    @Override
    public void verify(LedgerTransaction tx) {
        // Constraints on the inputs, outputs and commands.
        ContractsDSL.requireThat(req -> {
            req.using("The transaction should have no inputs",
                    tx.getInputStates().isEmpty());
            req.using("The transaction should have an AgreementState output",
                    tx.outputsOfType(AgreementState.class).size() == 1);
            req.using("The transaction should have no other outputs",
                    tx.getOutputs().size() == 1);
            req.using("The transaction should have an Agree command",
                    tx.commandsOfType(Commands.Agree.class).size() == 1);
            req.using("The transaction should have no other commands",
                    tx.getCommands().size() == 1);
            return null;
        });

        // Constraints on the included attachments.
        List<Attachment> nonContractAttachments = tx.getAttachments()
                .stream()
                .filter(p -> !(p instanceof ContractAttachment))
                .map(p -> (Attachment) p)
                .collect(Collectors.toList());

        Attachment attached = nonContractAttachments.get(0);

        ContractsDSL.requireThat(req -> {
            req.using("The transaction should have a single non-contract attachment",
                    nonContractAttachments.size() == 1);

            // TODO: Switch to constraint on the jar's signer.
            // In the future, Corda will support singing of jars. We will then be able to restrict
            // the attachments used to just those signed by party X.
            req.using("The jar's hash should be correct", attached.getId().equals(BLACKLIST_JAR_HASH));
            return null;
        });

        // Extract the blacklisted company names from the JAR.
        List<String> blacklistedCompanies = new ArrayList<>();
        JarInputStream attachmentJar = attached.openAsJAR();
        try {
            while (!attachmentJar.getNextEntry().getName().equals("blacklist.txt")) {
                // Calling 'getNextEntry()' causes us to scroll through the JAR.
            }
            InputStreamReader isrBlacklist = new InputStreamReader(attachmentJar, Charsets.UTF_8);
            BufferedReader brBlacklist = new BufferedReader(isrBlacklist, (8 * 1024)); // Note - changed BIR to BR

            String company = brBlacklist.readLine();

            while (company != null) {
                blacklistedCompanies.add(company);
                company = brBlacklist.readLine();
            }
        } catch (IOException e) {
            System.out.println("error reading blacklist.txt");
        }

        // Constraints on the blacklisted parties
        AgreementState agreement = tx.outputsOfType(AgreementState.class).get(0);

        List<Party> participants = new ArrayList<>();
        List<String> participantsOrgs = new ArrayList<>();
        for (AbstractParty p : agreement.getParticipants()) {
            Party participant = (Party) p;
            participantsOrgs.add(participant.getName().getOrganisation());
            participants.add(participant);
        }

        // overlap is whether any participants in the transaction belong to a blacklisted org.
        Set<String> overlap = new HashSet<>(blacklistedCompanies);
        overlap.retainAll(new HashSet<>(participantsOrgs)); // intersection

        ContractsDSL.requireThat(req -> {
            req.using("The agreement involved blacklisted parties" + overlap.toString(),
                    overlap.isEmpty());
            return null;
        });

        // Constraints on the signers.
        Command<Commands.Agree> command = tx.getCommand(0);
        List<PublicKey> particpantKeys = new ArrayList<>();
        for (Party p : participants) {
            particpantKeys.add(p.getOwningKey());
        }

        ContractsDSL.requireThat(req -> {
            req.using("All the parties to the agreement are signers",
                    command.getSigners().containsAll(particpantKeys));
            return null;
        });


    }

    public interface Commands extends CommandData {
        class Agree extends TypeOnlyCommandData implements Commands {}
    }
}
