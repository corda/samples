package com.example.test.contract

import com.example.contract.IOUContract
import com.example.state.IOUState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class IOUContractTests {
    private val ledgerServices = MockServices(listOf("com.example.contract", "com.example.flow"))
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val iouValue = 1

    @Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState(iouValue, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState(-1, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}