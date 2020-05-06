package com.example.service

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class RateOf(val contractor: Party, val company: Party)