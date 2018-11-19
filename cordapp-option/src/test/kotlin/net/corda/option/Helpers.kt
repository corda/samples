package net.corda.option

import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.utilities.days
import net.corda.option.base.COMPANY_STOCK_1
import net.corda.option.base.OPTION_CURRENCY
import net.corda.option.base.OptionType
import net.corda.option.base.state.OptionState
import java.time.Instant

fun createOption(issuer: Party, owner: Party) = OptionState(
        strikePrice = Amount(10, OPTION_CURRENCY),
        expiryDate = Instant.now() + 30.days,
        underlyingStock = COMPANY_STOCK_1,
        optionType = OptionType.PUT,
        issuer = issuer,
        owner = owner,
        linearId = DUMMY_LINEAR_ID
)

fun createBadOption(issuer: Party, owner: Party) = OptionState(
        strikePrice = Amount(10, OPTION_CURRENCY),
        // An option with an expiry date in the past is invalid.
        expiryDate = Instant.now() - 30.days,
        underlyingStock = COMPANY_STOCK_1,
        optionType = OptionType.PUT,
        issuer = issuer,
        owner = owner,
        linearId = DUMMY_LINEAR_ID
)