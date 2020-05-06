package com.example.service;

import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class RateOf {
    private Party contractor;
    private Party company;

    public RateOf(Party contractor, Party company) {
        this.contractor = contractor;
        this.company = company;
    }

    public Party getContractor() {
        return contractor;
    }

    public Party getCompany() {
        return company;
    }
}
