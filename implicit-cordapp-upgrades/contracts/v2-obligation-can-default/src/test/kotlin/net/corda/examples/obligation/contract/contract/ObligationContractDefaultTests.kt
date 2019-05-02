package net.corda.examples.obligation.contract.contract

import net.corda.core.identity.CordaX500Name
import net.corda.examples.obligation.contract.Obligation
import net.corda.examples.obligation.contract.ObligationContract
import net.corda.examples.obligation.contract.ObligationContract.Companion.OBLIGATION_CONTRACT_ID
import net.corda.finance.DOLLARS
import net.corda.testing.contracts.DummyContract
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.ledger
import org.junit.Test

class ObligationContractDefaultTests : ObligationContractUnitTests() {

    @Test
    fun `must handle multiple command values`() {
        ledgerServices.ledger {
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                this `fails with` "Required net.corda.examples.obligation.contract.ObligationContract.Commands command"
            }
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                verifies()
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                verifies()
            }
        }
    }

    @Test
    fun `must have one input and one output`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                input(DummyContract.PROGRAM_ID, DummyState())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "An obligation default transaction should only consume one input state."
            }
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "An obligation default transaction should only consume one input state."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "An obligation default transaction should only create one output state."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                output(OBLIGATION_CONTRACT_ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "An obligation default transaction should only create one output state."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                verifies()
            }
        }
    }

    @Test
    fun `only if defaulted may change`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party))
                output(OBLIGATION_CONTRACT_ID, Obligation(1.DOLLARS, alice.party, bob.party))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "Only the defaulted property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party))
                output(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, charlie.party))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "Only the defaulted property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party, 5.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party, 10.DOLLARS))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "Only the defaulted property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "Only the defaulted property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                verifies()
            }
        }
    }

    @Test
    fun `defaulted must change`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "The default property must be set to true on output."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                this `fails with` "The default property must not have been true on input."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                verifies()
            }
        }
    }

    @Test
    fun `all participants must sign`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, charlie.publicKey), ObligationContract.Commands.Default())
                this `fails with` "The borrower and lender only must sign an obligation default transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(bob.publicKey, charlie.publicKey), ObligationContract.Commands.Default())
                this `fails with` "The borrower and lender only must sign an obligation default transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Default())
                this `fails with` "The borrower and lender only must sign an obligation default transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withDefaulted())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Default())
                verifies()
            }
        }
    }
}