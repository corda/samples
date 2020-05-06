package com.example.test.contract

import com.example.contract.InvoiceContract
import com.example.state.InvoiceState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.LocalDate

class InvoiceContractTests {
    private val ledgerServices = MockServices(listOf("com.example.contract", "com.example.flow"))
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Contractor", "New York", "US"))
    private val oracle = TestIdentity(CordaX500Name("Oracle", "New York", "US"))
    private val signers = listOf(megaCorp.publicKey, contractor.publicKey, oracle.publicKey)
    private val date = LocalDate.now()
    private val rate = 10.0
    private val invoiceValue = 1

    @Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                fails()
                command(signers, InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                command(signers, InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                `fails with`("No inputs should be consumed when issuing an invoice.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                command(signers, InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `contractor must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                command(listOf(megaCorp.publicKey, oracle.publicKey), InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `company must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, contractor.party, megaCorp.party, oracle.party))
                command(listOf(contractor.publicKey, oracle.publicKey), InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `contractor is not company`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, InvoiceState(date, invoiceValue, rate, megaCorp.party, megaCorp.party, oracle.party))
                command(signers, InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value Invoices`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, InvoiceState(date, -1, rate, contractor.party, megaCorp.party, oracle.party))
                command(signers, InvoiceContract.Commands.Create(contractor.party, megaCorp.party, rate))
                `fails with`("The Invoice's value must be non-negative.")
            }
        }
    }
}