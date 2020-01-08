package com.example.service;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class Rate {
    private RateOf of;
    private Double val;

    Rate(RateOf of, Double val){
        this.of = of;
        this.val = val;
    }

    public RateOf getOf() {
        return of;
    }

    public Double getVal() {
        return val;
    }
}
