package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * An InvoiceState schema.
 */
public class InvoiceSchemaV1 extends MappedSchema {
    private int version = 1;
    private List<?> mappedTypes = ImmutableList.of(PersistentInvoice.class);
    private Class<?> schemaFamily = InvoiceSchema.class;
    public InvoiceSchemaV1(@NotNull Class<?> schemaFamily, int version, @NotNull Iterable<? extends Class<?>> mappedTypes) {
        super(schemaFamily, version, mappedTypes);
    }

    public InvoiceSchemaV1(){
        this(InvoiceSchema.class, 1, ImmutableList.of(PersistentInvoice.class));
    }


    @Entity
    @Table(name="iou_states")
    public static class PersistentInvoice extends PersistentState {
        @Column(name="contractor")
        private String contractorName;

        @Column(name="company")
        private String companyName;

        @Column(name="date")
        private LocalDate date;

        @Column(name ="hoursWorked")
        private int hoursWorked;

        @Column(name ="rate")
        private Double rate;

        @Column(name="linear_id")
        private UUID linearId;

        public PersistentInvoice(String contractorName, String companyName, LocalDate date,
                          int hoursWorked, Double rate, UUID linearId){
            this.contractorName = contractorName;
            this.companyName = companyName;
            this.date = date;
            this.hoursWorked = hoursWorked;
            this.rate = rate;
            this.linearId = linearId;
        }

        public PersistentInvoice(){
            this("", "", LocalDate.MIN, 0, 0.0, UUID.randomUUID());
        }


    }



}