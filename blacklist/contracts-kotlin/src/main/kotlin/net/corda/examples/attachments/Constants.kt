package net.corda.examples.attachments

const val BLACKLIST_JAR_PATH = "../contracts-kotlin/src/main/resources/blacklist.jar"
const val ATTACHMENT_FILE_NAME = "blacklist.txt"
val ATTACHMENT_EXPECTED_CONTENTS = listOf(
        "Crossland Savings",
        "TCF National Bank Wisconsin",
        "George State Bank",
        "The James Polk Stone Community Bank",
        "Tifton Banking Company")
val BLACKLISTED_PARTIES = listOf(
        "Crossland Savings",
        "TCF National Bank Wisconsin",
        "George State Bank",
        "The James Polk Stone Community Bank",
        "Tifton Banking Company"
)
// This jar exists, but does not meet the constraints imposed by AttachmentContract.
const val INCORRECT_JAR_PATH = "src/test/resources/invalid.jar"