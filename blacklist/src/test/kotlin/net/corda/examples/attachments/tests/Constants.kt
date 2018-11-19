package net.corda.examples.attachments.tests

// These blacklisted parties are named in the file blacklisted.txt file in blacklist.jar.
val BLACKLISTED_PARTIES = listOf(
        "Crossland Savings",
        "TCF National Bank Wisconsin",
        "George State Bank",
        "The James Polk Stone Community Bank",
        "Tifton Banking Company"
)
// This jar exists, but does not meet the constraints imposed by AttachmentContract.
const val INCORRECT_JAR_PATH = "src/test/resources/invalid.jar"