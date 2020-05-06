package net.corda.examples.obligation.contract

import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.utilities.toBase58String
import java.util.*

@BelongsToContract(ObligationContract::class)
data class Obligation(val amount: Amount<Currency>,
                      val lender: AbstractParty,
                      val borrower: AbstractParty,
                      val paid: Amount<Currency> = Amount(0, amount.token),
                      override val linearId: UniqueIdentifier = UniqueIdentifier(),
                      // V2: Add this new property to the contract state. This property must be nullable to ensure that
                      // the new version of the contracts jar can be used to spend states created by the old version.
                      val defaulted: Boolean? = null) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid + amountToPay)
    fun withNewLender(newLender: AbstractParty) = copy(lender = newLender)
    fun withoutLender() = copy(lender = NullKeys.NULL_PARTY)

    fun withDefaulted() = copy(defaulted = true)
    fun withoutDefaulted() = copy(defaulted = null)

    init {
        check(amount.token == paid.token) {
            "Require the same currency type for the amount owed and the amount paid, but got ${amount.token} for owed and ${paid.token} for paid"
        }
    }

    override fun toString(): String {
        val lenderString = (lender as? Party)?.name?.organisation ?: lender.owningKey.toBase58String()
        val borrowerString = (borrower as? Party)?.name?.organisation ?: borrower.owningKey.toBase58String()
        return "Obligation($linearId): $borrowerString owes $lenderString $amount and has paid $paid so far."
    }
}