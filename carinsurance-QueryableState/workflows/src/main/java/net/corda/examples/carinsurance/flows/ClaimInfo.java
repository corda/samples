package net.corda.examples.carinsurance.flows;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class ClaimInfo {
    private final String claimNumber;
    private final String claimDescription;
    private final int claimAmount;

    public ClaimInfo(String claimNumber, String claimDescription, int claimAmount) {
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
