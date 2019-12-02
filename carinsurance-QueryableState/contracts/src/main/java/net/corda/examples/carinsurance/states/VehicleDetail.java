package net.corda.examples.carinsurance.states;


import net.corda.core.serialization.CordaSerializable;

/**
 * Simple POJO class for the vehicle details.
 * Corda uses its own serialization framework hence the class needs to be annotated with @CordaSerializable, so that
 * the objects of the class can be serialized to be passed across different nodes.
 */
@CordaSerializable
public class VehicleDetail {

    private final String registrationNumber;
    private final String chasisNumber;
    private final String make;
    private final String model;
    private final String variant;
    private final String color;
    private final String fuelType;

    public VehicleDetail(String registrationNumber, String chasisNumber, String make, String model, String variant,
                         String color, String fuelType) {
        this.registrationNumber = registrationNumber;
        this.chasisNumber = chasisNumber;
        this.make = make;
        this.model = model;
        this.variant = variant;
        this.color = color;
        this.fuelType = fuelType;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getChasisNumber() {
        return chasisNumber;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getVariant() {
        return variant;
    }

    public String getColor() {
        return color;
    }

    public String getFuelType() {
        return fuelType;
    }
}