package net.corda.examples.obligation.contract

import net.corda.core.identity.CordaX500Name
import net.corda.examples.obligation.Obligation
import net.corda.examples.obligation.ObligationContract
import net.corda.examples.obligation.ObligationContract.Companion.OBLIGATION_CONTRACT_ID
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.finance.SWISS_FRANCS
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.ledger
import org.junit.Test

class ObligationContractIssueTests : ObligationContractUnitTests() {

    @Test
    fun `issue obligation transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "No inputs should be consumed when issuing an obligation."
            }
            transaction {
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                verifies() // As there are no input states.
            }
        }
    }

    @Test
    fun `Issue transaction must have only one output obligation`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation) // Two outputs fails.
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "Only one obligation state should be created when issuing an obligation."
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation) // One output passes.
                verifies()
            }
        }
    }

    @Test
    fun `cannot issue zero value obligations`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, Obligation(0.POUNDS, alice.party, bob.party)) // Zero amount fails.
                this `fails with` "A newly issued obligation must have a positive amount."
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, Obligation(100.SWISS_FRANCS, alice.party, bob.party))
                verifies()
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, Obligation(1.POUNDS, alice.party, bob.party))
                verifies()
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, Obligation(10.DOLLARS, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `lender and borrower must sign issue obligation transaction`() {
        val dummyIdentity = TestIdentity(CordaX500Name("Dummy", "", "GB"))

        ledgerServices.ledger {
            transaction {
                command(dummyIdentity.publicKey, ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "Both lender and borrower together only may sign obligation issue transaction."
            }
            transaction {
                command(alice.publicKey, ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "Both lender and borrower together only may sign obligation issue transaction."
            }
            transaction {
                command(bob.publicKey, ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "Both lender and borrower together only may sign obligation issue transaction."
            }
            transaction {
                command(listOf(bob.publicKey, bob.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "Both lender and borrower together only may sign obligation issue transaction."
            }
            transaction {
                command(listOf(bob.publicKey, bob.publicKey, dummyIdentity.publicKey, alice.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                this `fails with` "Both lender and borrower together only may sign obligation issue transaction."
            }
            transaction {
                command(listOf(bob.publicKey, bob.publicKey, bob.publicKey, alice.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                verifies()
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                verifies()
            }
        }
    }

    @Test
    fun `lender and borrower cannot be the same`() {
        val borrowerIsLenderObligation = Obligation(10.POUNDS, alice.party, alice.party)
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, borrowerIsLenderObligation)
                this `fails with` "The lender and borrower cannot be the same identity."
            }
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Issue())
                output(OBLIGATION_CONTRACT_ID, oneDollarObligation)
                verifies()
            }
        }
    }
}