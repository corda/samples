package net.corda.option.base

import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.finance.USD
import java.time.Instant

val ORACLE_NAME = CordaX500Name("Oracle", "New York","US")
// TODO: Move towards generating a price for each date based on a seed.
val DUMMY_CURRENT_DATE = Instant.parse("2017-07-03T10:15:30.00Z")!!
val OPTION_CURRENCY = USD
const val RISK_FREE_RATE = 0.01

const val COMPANY_STOCK_1 = "The Carlsbad National Bank"
const val COMPANY_STOCK_2 = "Wilburton State Bank"
const val COMPANY_STOCK_3 = "De Soto State Bank"
const val COMPANY_STOCK_4 = "Florida Traditions Bank"
const val COMPANY_STOCK_5 = "CorEast Federal Savings Bank"

val COMPANY_AMOUNT_1 = Amount(300, OPTION_CURRENCY)
val COMPANY_AMOUNT_2 = Amount(500, OPTION_CURRENCY)
val COMPANY_AMOUNT_3 = Amount(400, OPTION_CURRENCY)
val COMPANY_AMOUNT_4 = Amount(100, OPTION_CURRENCY)
val COMPANY_AMOUNT_5 = Amount(200, OPTION_CURRENCY)

const val COMPANY_VOLATILITY_1 = 0.40
const val COMPANY_VOLATILITY_2 = 0.05
const val COMPANY_VOLATILITY_3 = 0.25
const val COMPANY_VOLATILITY_4 = 0.70
const val COMPANY_VOLATILITY_5 = 0.15

val KNOWN_SPOTS = listOf(
        SpotPrice(COMPANY_STOCK_1, DUMMY_CURRENT_DATE, COMPANY_AMOUNT_1),
        SpotPrice(COMPANY_STOCK_2, DUMMY_CURRENT_DATE, COMPANY_AMOUNT_2),
        SpotPrice(COMPANY_STOCK_3, DUMMY_CURRENT_DATE, COMPANY_AMOUNT_3),
        SpotPrice(COMPANY_STOCK_4, DUMMY_CURRENT_DATE, COMPANY_AMOUNT_4),
        SpotPrice(COMPANY_STOCK_5, DUMMY_CURRENT_DATE, COMPANY_AMOUNT_5)
)
val KNOWN_VOLATILITIES = listOf(
        Volatility(COMPANY_STOCK_1, DUMMY_CURRENT_DATE, COMPANY_VOLATILITY_1),
        Volatility(COMPANY_STOCK_2, DUMMY_CURRENT_DATE, COMPANY_VOLATILITY_2),
        Volatility(COMPANY_STOCK_3, DUMMY_CURRENT_DATE, COMPANY_VOLATILITY_3),
        Volatility(COMPANY_STOCK_4, DUMMY_CURRENT_DATE, COMPANY_VOLATILITY_4),
        Volatility(COMPANY_STOCK_5, DUMMY_CURRENT_DATE, COMPANY_VOLATILITY_5)
)