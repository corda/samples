package net.corda.examples.attachments.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateTests {

    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB")).party

    @Test
    fun `agreementStateHasParamsOfCorrectTypeInConstructor`() {
        AgreementState(alice, bob, "this is an agreement txt")
    }

    @Test
    fun `agreementStateHasGettersForPartyAPartyBTxt`() {
        val agreementState = AgreementState(alice, bob, "this is an agreement txt")
        assertEquals(alice, agreementState.partyA)
        assertEquals(bob, agreementState.partyB)
        assertEquals("this is an agreement txt", agreementState.txt)
    }

    @Test
    fun `agreementStateHasTwoParticipantsPartyAPartyB`() {
        val agreementState = AgreementState(alice, bob, "this is an agreement txt")
        assertEquals(2, agreementState.participants.size)
        assertTrue(agreementState.participants.contains(alice))
        assertTrue(agreementState.participants.contains(bob))
    }
}