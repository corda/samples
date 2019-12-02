package net.corda.examples.carinsurance.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;


/**
 * JPA Entity for saving insurance details to the database table
 */
@Entity
@Table(name = "INSURANCE_DETAIL")
public class PersistentInsurance extends PersistentState implements Serializable {

    @Column private final String policyNumber;
    @Column private final Long insuredValue;
    @Column private final Integer duration;
    @Column private final Integer premium;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "id", referencedColumnName = "id"),
            @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber"),
    })
    private final PersistentVehicle vehicle;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "output_index", referencedColumnName = "output_index"),
            @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id"),
    })
    private List<PersistentClaim> claims;

    /**
     * Default constructor required by Hibernate
     */
    public PersistentInsurance() {
        this.policyNumber = null;
        this.insuredValue = null;
        this.duration = null;
        this.premium = null;
        this.vehicle = null;
        this.claims = null;
    }

    public PersistentInsurance(String policyNumber, Long insuredValue, Integer duration, Integer premium, PersistentVehicle vehicle,
                               List<PersistentClaim> claims) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.vehicle = vehicle;
        this.claims = claims;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Long getInsuredValue() {
        return insuredValue;
    }

    public Integer getPremium() {
        return premium;
    }

    public Integer getDuration() {
        return duration;
    }

    public PersistentVehicle getVehicle() {
        return vehicle;
    }

    public List<PersistentClaim> getClaims() {
        return claims;
    }
}
