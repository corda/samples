package com.example.service

import net.corda.core.contracts.CommandData
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Rate(val of: RateOf, val value: Double) : CommandData
