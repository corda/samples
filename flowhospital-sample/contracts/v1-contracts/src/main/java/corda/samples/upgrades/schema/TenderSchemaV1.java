package corda.samples.upgrades.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

public class TenderSchemaV1 extends MappedSchema {

    public TenderSchemaV1() {
        super(TenderSchema.class, 1, ImmutableList.of(PersistentTender.class));
    }

    @Entity
    @Table(name = "tender")
    public static class PersistentTender extends PersistentState {

        @Column
        private Party tenderingOrganisation;
        @Column
        private String tenderName;
        @Column
        private Integer tenderId;

        public PersistentTender() {
            this.tenderingOrganisation = null;
            this.tenderName = null;
            this.tenderId = null;
        }

        public PersistentTender(Party tenderingOrganisation, String tenderName, Integer tenderId) {
            this.tenderingOrganisation = tenderingOrganisation;
            this.tenderName = tenderName;
            this.tenderId = tenderId;
        }

        public Party getTenderingOrganisation() {
            return tenderingOrganisation;
        }

        public String getTenderName() {
            return tenderName;
        }

        public Integer getTenderId() {
            return tenderId;
        }
    }
}
