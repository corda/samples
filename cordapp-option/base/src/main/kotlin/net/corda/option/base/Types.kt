package net.corda.option.base

import net.corda.core.contracts.Amount
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*

/** Represents the price of a given stock at a given point in time. */
@CordaSerializable
data class SpotPrice(val stock: String, val atTime: Instant, val value: Amount<Currency>)

/** Represents the historic volatility of a given stock at a given point in time. */
@CordaSerializable
data class Volatility(val stock: String, val atTime: Instant, val value: Double)

/** Represents a stock at a given point in time. */
@CordaSerializable
enum class OptionType { CALL, PUT }