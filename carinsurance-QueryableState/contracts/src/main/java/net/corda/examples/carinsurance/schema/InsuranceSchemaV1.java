package net.corda.examples.carinsurance.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;

/**
 * MappedSchema subclass representing the custom schema for the Insurance QueryableState.
 */
public class InsuranceSchemaV1 extends MappedSchema {

    /**
     * The constructor of the MappedSchema requires the schemafamily, verison, and a list of all JPA entity classes for
     * the Schema.
     */
    public InsuranceSchemaV1() {
        super(InsuranceSchemaFamily.class, 1, ImmutableList.of(PersistentInsurance.class,
                PersistentVehicle.class, PersistentClaim.class));
    }
}