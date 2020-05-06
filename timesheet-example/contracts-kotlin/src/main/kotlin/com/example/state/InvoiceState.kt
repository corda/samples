package com.example.state

import com.example.contract.InvoiceContract
import com.example.schema.InvoiceSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.LocalDate

/**
 * The state object recording invoice of billable hours from the contractor
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param hoursWorked the total hours worked for the given date
 * @param date the date on which the work occurred
 * @param contractor the party issuing the invoice
 * @param company the party receiving and approving the invoice.
 */
@BelongsToContract(InvoiceContract::class)
data class InvoiceState(val date: LocalDate,
                        val hoursWorked: Int,
                        val rate: Double,
                        val contractor: Party,
                        val company: Party,
                        val oracle: Party,
                        val paid: Boolean = false,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(contractor, company)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is InvoiceSchemaV1 -> InvoiceSchemaV1.PersistentInvoice(
                    this.contractor.name.toString(),
                    this.company.name.toString(),
                    this.date,
                    this.hoursWorked,
                    this.rate,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(InvoiceSchemaV1)
}
