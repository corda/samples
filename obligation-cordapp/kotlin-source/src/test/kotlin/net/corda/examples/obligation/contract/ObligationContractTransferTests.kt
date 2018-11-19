package net.corda.examples.obligation.contract

import net.corda.core.identity.CordaX500Name
import net.corda.examples.obligation.Obligation
import net.corda.examples.obligation.ObligationContract
import net.corda.examples.obligation.ObligationContract.Companion.OBLIGATION_CONTRACT_ID
import net.corda.finance.DOLLARS
import net.corda.testing.contracts.DummyContract
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.ledger
import org.junit.Test

class ObligationContractTransferTests : ObligationContractUnitTests() {

    @Test
    fun `must handle multiple command values`() {
        ledgerServices.ledger {
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand())
                this `fails with` "Required net.corda.examples.obligation.ObligationContract.Commands command"
            }
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                verifies()
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
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
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "An obligation transfer transaction should only consume one input state."
            }
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "An obligation transfer transaction should only consume one input state."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "An obligation transfer transaction should only create one output state."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                output(OBLIGATION_CONTRACT_ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "An obligation transfer transaction should only create one output state."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                verifies()
            }
        }
    }

    @Test
    fun `only the lender may change`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party))
                output(OBLIGATION_CONTRACT_ID, Obligation(1.DOLLARS, alice.party, bob.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "Only the lender property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party))
                output(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "Only the lender property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party, 5.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party, 10.DOLLARS))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "Only the lender property may change."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                verifies()
            }
        }
    }

    @Test
    fun `the lender must change`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "The lender property must change in a transfer."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                verifies()
            }
        }
    }

    @Test
    fun `all participants must sign`() {
        val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "", "GB"))

        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "The borrower, old lender and new lender only must sign an obligation transfer transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "The borrower, old lender and new lender only must sign an obligation transfer transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "The borrower, old lender and new lender only must sign an obligation transfer transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, miniCorp.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "The borrower, old lender and new lender only must sign an obligation transfer transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey, miniCorp.publicKey), ObligationContract.Commands.Transfer())
                this `fails with` "The borrower, old lender and new lender only must sign an obligation transfer transaction"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.party))
                command(listOf(alice.publicKey, bob.publicKey, charlie.publicKey), ObligationContract.Commands.Transfer())
                verifies()
            }
        }
    }
}
