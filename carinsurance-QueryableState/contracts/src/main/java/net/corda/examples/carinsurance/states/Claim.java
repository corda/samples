package net.corda.examples.carinsurance.states;

import net.corda.core.serialization.CordaSerializable;

/**
 * Simple POJO class for the claim details.
 * Corda uses its own serialization framework hence the class needs to be annotated with @CordaSerializable, so that
 * the objects of the class can be serialized to be passed across different nodes.
 */
@CordaSerializable
public class Claim {

    private final String claimNumber;
    private final String claimDescription;
    private final int claimAmount;

    public Claim(String claimNumber, String claimDescription, int claimAmount) {
        this.claimNumber = claimNumber;
        this.claimDescription = claimDescription;
        this.claimAmount = claimAmount;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public int getClaimAmount() {
        return claimAmount;
    }
}
