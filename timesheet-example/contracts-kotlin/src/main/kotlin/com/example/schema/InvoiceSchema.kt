package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for InvoiceState.
 */
object InvoiceSchema

/**
 * An InvoiceState schema.
 */
object InvoiceSchemaV1 : MappedSchema(
        schemaFamily = InvoiceSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentInvoice::class.java)) {
    @Entity
    @Table(name = "iou_states")
    class PersistentInvoice(
            @Column(name = "contractor")
            var contractorName: String,

            @Column(name = "company")
            var companyName: String,

            @Column(name = "date")
            var date: LocalDate,

            @Column(name = "hoursWorked")
            var hoursWorked: Int,

            @Column(name = "rate")
            var rate: Double,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", LocalDate.MIN, 0, 0.0, UUID.randomUUID())
    }
}