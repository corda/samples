package net.corda.option.base.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.option.base.OptionType
import net.corda.option.base.RISK_FREE_RATE
import net.corda.option.base.Volatility
import net.corda.option.base.contract.OptionContract
import net.corda.option.base.pricingmodel.BlackScholes
import java.time.Instant
import java.util.*

/**
 * Models an (American) option contract.
 *
 * @property strikePrice price at which the [underlyingStock] can be purchased or sold.
 * @property expiryDate time after which option cannot be exercised.
 * @property underlyingStock the stock that the option allows you to buy or sell.
 * @property issuer the entity who agrees to buy or sell the underlying from the option's owner.
 * @property owner the current owner of the option.
 * @property optionType either CALL (option to buy the stock at the strike price) or PUT (option to sell the stock at
 *   the strike price).
 * @property spotPriceAtPurchase price at which the option was purchased.
 */
@BelongsToContract(OptionContract::class)
data class OptionState(
        val strikePrice: Amount<Currency>,
        val expiryDate: Instant,
        val underlyingStock: String,
        val issuer: Party,
        val owner: Party,
        val optionType: OptionType,
        var spotPriceAtPurchase: Amount<Currency> = Amount(0, strikePrice.token),
        val exercised: Boolean = false,
        val exercisedOnDate: Instant? = null,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    companion object {
        // TODO: Do not delete. Will be used in the implementation of the redeem flow.
        fun calculateMoneyness(strike: Amount<Currency>, spot: Amount<Currency>, optionType: OptionType): Amount<Currency> {
            val zeroAmount = Amount.zero(spot.token)
            when {
                optionType == OptionType.CALL -> {
                    if (strike >= spot)
                        return zeroAmount
                    return spot - strike
                }
                spot >= strike -> return zeroAmount
                else -> return strike - spot
            }
        }

        fun calculatePremium(optionState: OptionState, volatility: Volatility): Amount<Currency> {
            val blackScholes = BlackScholes(optionState.spotPriceAtPurchase.quantity.toDouble(), optionState.strikePrice.quantity.toDouble(), RISK_FREE_RATE, 100.toDouble(), volatility.value)
            val value = if (optionState.optionType == OptionType.CALL) {
                blackScholes.BSCall().toLong() * 100
            } else {
                blackScholes.BSPut().toLong() * 100
            }
            return Amount(value, optionState.strikePrice.token)
        }
    }

    override val participants get() = listOf(owner, issuer)

    override fun toString() = "${this.optionType.name} option on ${this.underlyingStock} at strike ${this.strikePrice} expiring on ${this.expiryDate}"
}