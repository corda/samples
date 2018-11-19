package net.corda.examples.obligation.contract

import net.corda.core.contracts.Amount
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.examples.obligation.Obligation
import net.corda.examples.obligation.ObligationContract
import net.corda.examples.obligation.ObligationContract.Companion.OBLIGATION_CONTRACT_ID
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class ObligationContractSettleTests : ObligationContractUnitTests() {
    private val issuer = TestIdentity(CordaX500Name("MegaBank", "", "US"))
    private val defaultRef = Byte.MAX_VALUE
    private val defaultIssuer = issuer.ref(defaultRef)

    private fun createCashState(amount: Amount<Currency>, owner: AbstractParty): Cash.State {
        return Cash.State(amount = amount `issued by` defaultIssuer, owner = owner)
    }

    @Test
    fun `must include settle command`() {
        val inputCash = createCashState(5.DOLLARS, bob.party)
        val outputCash = inputCash.withNewOwner(newOwner = alice.party).ownableState
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                input(OBLIGATION_CONTRACT_ID, inputCash)
                output(OBLIGATION_CONTRACT_ID, outputCash)
                command(bob.publicKey, Cash.Commands.Move())
                this.fails()
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                input(OBLIGATION_CONTRACT_ID, inputCash)
                output(OBLIGATION_CONTRACT_ID, outputCash)
                command(bob.publicKey, Cash.Commands.Move())
                command(listOf(alice.publicKey, bob.publicKey), DummyCommand()) // Wrong type.
                this.fails()
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                input(OBLIGATION_CONTRACT_ID, inputCash)
                output(OBLIGATION_CONTRACT_ID, outputCash)
                command(bob.publicKey, Cash.Commands.Move())
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle()) // Correct Type.
                verifies()
            }
        }
    }

    @Test
    fun `must have only one input obligation`() {
        val duplicateObligation = Obligation(10.DOLLARS, alice.party, bob.party)
        val tenDollars = createCashState(10.DOLLARS, bob.party)
        val fiveDollars = createCashState(5.DOLLARS, bob.party)
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                this `fails with` "There must be one input obligation."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, duplicateObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, Cash.Commands.Move())
                this `fails with` "There must be one input obligation."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                input(OBLIGATION_CONTRACT_ID, tenDollars)
                output(OBLIGATION_CONTRACT_ID, tenDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, Cash.Commands.Move())
                verifies()
            }
        }
    }

    @Test
    fun `must be cash output states present`() {
        val cash = createCashState(5.DOLLARS, bob.party)
        val cashPayment = cash.withNewOwner(newOwner = alice.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "There must be output cash."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, cash)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, cashPayment.ownableState)
                command(bob.publicKey, cashPayment.command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `must be cash output states with receipient as owner`() {
        val cash = createCashState(5.DOLLARS, bob.party)
        val invalidCashPayment = cash.withNewOwner(newOwner = charlie.party)
        val validCashPayment = cash.withNewOwner(newOwner = alice.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, cash)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, invalidCashPayment.ownableState)
                command(bob.publicKey, invalidCashPayment.command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "There must be output cash paid to the recipient."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, cash)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, validCashPayment.ownableState)
                command(bob.publicKey, validCashPayment.command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `cash settlement amount must be less than the remaining amount`() {
        val elevenDollars = createCashState(11.DOLLARS, bob.party)
        val tenDollars = createCashState(10.DOLLARS, bob.party)
        val fiveDollars = createCashState(5.DOLLARS, bob.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, elevenDollars)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(11.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, elevenDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, elevenDollars.withNewOwner(newOwner = alice.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "The amount settled cannot be more than the amount outstanding."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = alice.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, tenDollars)
                output(OBLIGATION_CONTRACT_ID, tenDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, tenDollars.withNewOwner(newOwner = alice.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `cash settlement must be in the correct currency`() {
        val tenDollars = createCashState(10.DOLLARS, bob.party)
        val tenPounds = createCashState(10.POUNDS, bob.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, tenPounds)
                output(OBLIGATION_CONTRACT_ID, tenPounds.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, tenPounds.withNewOwner(newOwner = alice.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "Token mismatch: GBP vs USD"
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, tenDollars)
                output(OBLIGATION_CONTRACT_ID, tenDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, tenDollars.withNewOwner(newOwner = alice.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `must have output obligation if not fully settling`() {
        val tenDollars = createCashState(10.DOLLARS, bob.party)
        val fiveDollars = createCashState(5.DOLLARS, bob.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "There must be one output obligation."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollars)
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(10.DOLLARS))
                output(OBLIGATION_CONTRACT_ID, tenDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, tenDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "There must be no output obligation as it has been fully settled."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollars)
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, tenDollars.withNewOwner(newOwner = alice.party).ownableState)
                command(bob.publicKey, tenDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `only paid property may change`() {
        val fiveDollars = createCashState(5.DOLLARS, bob.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.copy(borrower = alice.party, paid = 5.DOLLARS))
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "The borrower may not change when settling."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.copy(amount = 0.DOLLARS, paid = 5.DOLLARS))
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "The amount may not change when settling."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.copy(lender = charlie.party, paid = 5.DOLLARS))
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "The lender may not change when settling."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                input(OBLIGATION_CONTRACT_ID, fiveDollars)
                output(OBLIGATION_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = alice.party).ownableState)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                command(bob.publicKey, fiveDollars.withNewOwner(newOwner = bob.party).command)
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }

    @Test
    fun `must be signed by all participants`() {
        val cash = createCashState(5.DOLLARS, bob.party)
        val cashPayment = cash.withNewOwner(newOwner = alice.party)
        ledgerServices.ledger {
            transaction {
                input(OBLIGATION_CONTRACT_ID, cash)
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, cashPayment.ownableState)
                command(bob.publicKey, cashPayment.command)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                command(listOf(alice.publicKey, charlie.publicKey), ObligationContract.Commands.Settle())
                this `fails with` "Both lender and borrower together only must sign obligation settle transaction."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, cash)
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, cashPayment.ownableState)
                command(bob.publicKey, cashPayment.command)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                command(bob.publicKey, ObligationContract.Commands.Settle())
                this `fails with` "Both lender and borrower together only must sign obligation settle transaction."
            }
            transaction {
                input(OBLIGATION_CONTRACT_ID, cash)
                input(OBLIGATION_CONTRACT_ID, tenDollarObligation)
                output(OBLIGATION_CONTRACT_ID, cashPayment.ownableState)
                command(bob.publicKey, cashPayment.command)
                output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(5.DOLLARS))
                command(listOf(alice.publicKey, bob.publicKey), ObligationContract.Commands.Settle())
                verifies()
            }
        }
    }
}