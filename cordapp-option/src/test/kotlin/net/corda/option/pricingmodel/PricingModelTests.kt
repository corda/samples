package net.corda.option.pricingmodel

import net.corda.option.base.pricingmodel.BlackScholes
import org.junit.Test
import kotlin.test.assertEquals

class PricingModelTests {

    @Test
    fun blackScholesPricingModelReturnsCorrectPremiumForOutTheMoneyCallOption() {
        val spot = 500.toDouble()
        val strike = 550.toDouble()
        val riskFreeRate = 0.1
        val timeToExpiry = 0.40
        val volatility = 0.80

        val premium = Math.round(BlackScholes(spot, strike, riskFreeRate, volatility, timeToExpiry).BSCall())

        assertEquals(89, premium)
    }

    @Test
    fun blackScholesPricingModelReturnsCorrectPremiumForOutTheMoneyPutOption() {
        val spot = 400.toDouble()
        val strike = 350.toDouble()
        val riskFreeRate = 0.1
        val timeToExpiry = 0.60
        val volatility = 0.50

        val premium = Math.round(BlackScholes(spot, strike, riskFreeRate, volatility, timeToExpiry).BSPut())

        assertEquals(28, premium)
    }

    @Test
    fun blackScholesPricingModelReturnsCorrectPremiumForInTheMoneyCallOption() {
        val spot = 10.toDouble()
        val strike = 5.toDouble()
        val riskFreeRate = 0.01
        val timeToExpiry = 0.40
        val volatility = 0.2
        val premium = Math.round(BlackScholes(spot, strike, riskFreeRate, volatility, timeToExpiry).BSCall())
        assertEquals(5, premium)
    }

    @Test
    fun blackScholesPricingModelReturnsCorrectPremiumForInTheMoneyPutOption() {
        val spot = 50.toDouble()
        val strike = 100.toDouble()
        val riskFreeRate = 0.01
        val timeToExpiry = 0.20
        val volatility = 0.34
        val premium = Math.round(BlackScholes(spot, strike, riskFreeRate, volatility, timeToExpiry).BSPut())
        assertEquals(50, premium)
    }
}