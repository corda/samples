package net.corda.examples.attachments;

import net.corda.core.crypto.SecureHash;

import java.util.Arrays;
import java.util.List;

public interface Constants {
    String BLACKLIST_JAR_PATH = "../contracts-java/src/main/resources/blacklist.jar";
    SecureHash BLACKLIST_JAR_HASH = SecureHash.parse("4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A");
    String ATTACTMENT_FILE_NAME = "blacklist.txt";
    List<String> ATTACHMENT_EXPECTED_CONTENTS = Arrays.asList(
            "Crossland Savings",
            "TCF National Bank Wisconsin",
            "George State Bank",
            "The James Polk Stone Community Bank",
            "Tifton Banking Company"
    );
    List<String> BLACKLISTED_PARTIES = Arrays.asList(
            "Crossland Savings",
            "TCF National Bank Wisconsin",
            "George State Bank",
            "The James Polk Stone Community Bank",
            "Tifton Banking Company"
    );
    // This jar exists, but does not meet the constraints imposed by AttachmentContract.
    String INCORRECT_JAR_PATH = "src/test/resources/invalid.jar";
}