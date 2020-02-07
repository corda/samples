package net.corda.examples.attachments.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StateTests {
    private final Party alice = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();

    @Test
    public void agreementStateHasParamsOfCorrectTypeInConstructor() {
        new AgreementState(alice, bob, "this is an agreement txt");
    }

    @Test
    public void tokenStateHasGettersForPartyAPartyBandTxt() {
        AgreementState agreementState = new AgreementState(alice, bob, "this is an agreement txt");
        assertEquals(alice, agreementState.getPartyA());
        assertEquals(bob, agreementState.getPartyB());
        assertEquals("this is an agreement txt", agreementState.getTxt());
    }

    @Test
    public void agreementStateImplementsContractState() {
        assertTrue(new AgreementState(alice, bob, "this is an agreement txt") instanceof ContractState);
    }

    @Test
    public void agreementStateHasTwoParticipantsPartyAPartyB() {
        AgreementState agreementState = new AgreementState(alice, bob, "this is an agreement txt");
        assertEquals(2, agreementState.getParticipants().size());
        assertTrue(agreementState.getParticipants().contains(alice));
        assertTrue(agreementState.getParticipants().contains(bob));
    }
}