package net.corda.examples.attachments.client;

import kotlin.text.Charsets;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.crypto.SecureHash;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import static net.corda.examples.attachments.Constants.*;

public class Client {

    private static class Companion {
        static Logger logger = LoggerFactory.getLogger(Client.class);
    }

    /**
     * Uploads the jar of blacklisted counterparties with whom agreements cannot be struck to the node.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            String message = "Usage: uploadBlacklist <node address 1> <node address 2> ...";
            throw new IllegalArgumentException(message.toString());
        }

        for (String arg : args) {
            NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(arg);
            CordaRPCConnection rpcConnection = new CordaRPCClient(nodeAddress).start("user1", "test");
            CordaRPCOps proxy = rpcConnection.getProxy();

            SecureHash attachmentHash = BLACKLIST_JAR_HASH;

            // take relative path using substring of constant BLACKLIST_JAR_PATH, check if node contains blacklist already
            if (!proxy.attachmentExists(attachmentHash)) {
                System.out.println("Working Directory = " +
                        System.getProperty("user.dir"));
                attachmentHash = uploadAttachment(proxy, BLACKLIST_JAR_PATH);
                Companion.logger.info("Blacklist uploaded to node at " + nodeAddress);
            } else {
                Companion.logger.info("Node already contains Blacklist, skipping upload at " + nodeAddress);
            }

            JarInputStream attachmentJar = downloadAttachment(proxy, attachmentHash);
            Companion.logger.info("Blacklist downloaded from node at " + nodeAddress);

            checkAttachment(attachmentJar, ATTACTMENT_FILE_NAME, ATTACHMENT_EXPECTED_CONTENTS);
            Companion.logger.info("Attachment contents checked on node at " + nodeAddress);
        }

    }

    /**
     * Uploads the attachment at [attachmentPath] to the node.
     */
    private static SecureHash uploadAttachment(CordaRPCOps proxy, String attachmentPath) throws FileNotFoundException, FileAlreadyExistsException {
        FileInputStream attachmentUploadInputStream = new FileInputStream(new File(attachmentPath));
        return proxy.uploadAttachment(attachmentUploadInputStream);
    }

    /**
     * Downloads the attachment with hash [attachmentHash] from the node.
     */
    private static JarInputStream downloadAttachment(CordaRPCOps proxy, SecureHash attachmentHash) throws IOException {
        InputStream attachmentDownloadInputStream = proxy.openAttachment(attachmentHash);
        return new JarInputStream(attachmentDownloadInputStream);
    }

    /**
     * Checks the [expectedFileName] and [expectedContents] of the downloaded [attachmentJar].
     */
    private static void checkAttachment(JarInputStream attachmentJar, String expectedFileName, List<String> expectedContents) throws IOException {
        String name = attachmentJar.getNextEntry().getName();
        while (!name.equals(expectedFileName)) {
            name = attachmentJar.getNextEntry().getName();
        }

        BufferedInputStream bisAttachmentJar = new BufferedInputStream(attachmentJar, (8*1024));
        InputStreamReader isrAttachmentJar = new InputStreamReader(bisAttachmentJar, Charsets.UTF_8);
        BufferedReader brAttachmentJar = new BufferedReader(isrAttachmentJar);

        List<String> contents = brAttachmentJar.lines().collect(Collectors.toList());

        if (!contents.equals(expectedContents)) {
            throw new IllegalArgumentException("Downloaded JAR did not have the expected contents.");
        }

    }
}
