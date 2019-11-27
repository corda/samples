package net.corda.examples.carinsurance.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.examples.carinsurance.contracts.InsuranceContract;
import net.corda.examples.carinsurance.schema.InsuranceSchemaV1;
import net.corda.examples.carinsurance.schema.PersistentClaim;
import net.corda.examples.carinsurance.schema.PersistentInsurance;
import net.corda.examples.carinsurance.schema.PersistentVehicle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Insurance State
 * The state should implement the QueryableState to support custom schema development.
 */
@BelongsToContract(InsuranceContract.class)
public class InsuranceState implements QueryableState {

    // Represents the asset which is insured.
    // This will be used to demonstrate one-to-one relationship
    private final VehicleDetail vehicleDetail;

    // Fields related to the insurance state.
    private final String policyNumber;
    private final long insuredValue;
    private final int duration;
    private final int premium;

    private final Party insurer;
    private final Party insuree;

    // Insurance claims made against the insurace policy
    // This will be used to demonstrate one-to-many relationship
    private final List<Claim> claims;

    public InsuranceState(String policyNumber, long insuredValue, int duration, int premium, Party insurer,
                     Party insuree, VehicleDetail  vehicleDetail, List<Claim> claims) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.insurer = insurer;
        this.insuree = insuree;
        this.vehicleDetail = vehicleDetail;
        this.claims = claims;
    }

    /**
     * Used to Generate the Entity for this Queryable State.
     * This method is called by the SchemaService of the node, and the returned entity is handed over to the ORM tool
     * to be persisted in custom database table.
     *
     * @param schema
     * @return PersistentState
     */
    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if(schema instanceof InsuranceSchemaV1){

            // Create list of PersistentClaim entity against every Claims object.
            List<PersistentClaim> persistentClaims = new ArrayList<>();
            if(claims != null && claims.size() > 0) {
                for(Claim claim: claims){
                    PersistentClaim persistentClaim = new PersistentClaim(
                            claim.getClaimNumber(),
                            claim.getClaimDescription(),
                            claim.getClaimAmount()
                    );
                    persistentClaims.add(persistentClaim);
                }
            }

            return new PersistentInsurance(
                    this.policyNumber,
                    this.insuredValue,
                    this.duration,
                    this.premium,
                    this.vehicleDetail==null ? null : new PersistentVehicle(
                            vehicleDetail.getRegistrationNumber(),
                            vehicleDetail.getChasisNumber(),
                            vehicleDetail.getMake(),
                            vehicleDetail.getModel(),
                            vehicleDetail.getVariant(),
                            vehicleDetail.getColor(),
                            vehicleDetail.getFuelType()
                    ),
                    this.claims == null? null: persistentClaims
            );
        }else{
            throw new IllegalArgumentException("Unsupported Schema");
        }
    }

    /**
     * Returns a list of supported Schemas by this Queryable State.
     *
     * @return Iterable<MappedSchema>
     */
    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new InsuranceSchemaV1());
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(insuree, insurer);
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public long getInsuredValue() {
        return insuredValue;
    }

    public int getDuration() {
        return duration;
    }

    public int getPremium() {
        return premium;
    }

    public Party getInsurer() {
        return insurer;
    }

    public Party getInsuree() {
        return insuree;
    }

    public VehicleDetail getVehicleDetail() {
        return vehicleDetail;
    }

    public List<Claim> getClaims() {
        return claims;
    }
}
