package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * An IOUState schema.
 */
public class IOUSchemaV1 extends MappedSchema {
    public IOUSchemaV1() {
        super(IOUSchema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "iou_states")
    public static class PersistentIOU extends PersistentState {
        @Column(name = "lender") private final String lender;
        @Column(name = "borrower") private final String borrower;
        @Column(name = "value") private final int value;
        @Column(name = "linear_id") private final UUID linearId;


        public PersistentIOU(String lender, String borrower, int value, UUID linearId) {
            this.lender = lender;
            this.borrower = borrower;
            this.value = value;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentIOU() {
            this.lender = null;
            this.borrower = null;
            this.value = 0;
            this.linearId = null;
        }

        public String getLender() {
            return lender;
        }

        public String getBorrower() {
            return borrower;
        }

        public int getValue() {
            return value;
        }

        public UUID getId() {
            return linearId;
        }
    }
}