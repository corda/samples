package net.corda.examples.obligation.flows

import net.corda.core.contracts.withoutIssuer
import net.corda.core.flows.FlowException
import net.corda.examples.obligation.Obligation
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.StartedMockNode
import kotlin.test.*

class SettleObligationTests : ObligationTests() {

    // Helper for extracting the cash output owned by a the node.
    private fun getCashOutputByOwner(
            cashStates: List<Cash.State>,
            node: StartedMockNode): Cash.State {
        return cashStates.single { cashState ->
            val cashOwner = node.services.identityService.requireWellKnownPartyFromAnonymous(cashState.owner)
            cashOwner == node.info.chooseIdentity()
        }
    }

    @org.junit.Test
    fun `Settle flow can only be started by borrower`() {
        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        assertFailsWith<FlowException> {
            settleObligation(issuedObligation.linearId, b, 1000.POUNDS)
        }
    }

    @org.junit.Test
    fun `Settle flow fails when borrower has no cash`() {
        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        assertFailsWith<FlowException> {
            settleObligation(issuedObligation.linearId, a, 1000.POUNDS)
        }
    }

    @org.junit.Test
    fun `Settle flow fails when borrower pledges too much cash to settle`() {
        // Self issue cash.
        selfIssueCash(a, 1500.POUNDS)
        network.waitQuiescent()

        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        assertFailsWith<FlowException> {
            settleObligation(issuedObligation.linearId, a, 1500.POUNDS)
        }
    }

    @org.junit.Test
    fun `Fully settle non-anonymous obligation`() {
        // Self issue cash.
        selfIssueCash(a, 1500.POUNDS)
        network.waitQuiescent()

        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        val settleTransaction = settleObligation(issuedObligation.linearId, a, 1000.POUNDS)
        network.waitQuiescent()
        assert(settleTransaction.tx.outputsOfType<Obligation>().isEmpty())

        // Check both parties have the transaction.
        val aTx = a.services.validatedTransactions.getTransaction(settleTransaction.id)
        val bTx = b.services.validatedTransactions.getTransaction(settleTransaction.id)
        assertEquals(aTx, bTx)
    }

    @org.junit.Test
    fun `Fully settle anonymous obligation`() {
        // Self issue cash.
        selfIssueCash(a, 1500.POUNDS)

        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        val settleTransaction = settleObligation(issuedObligation.linearId, a, 1000.POUNDS)
        network.waitQuiescent()
        assert(settleTransaction.tx.outputsOfType<Obligation>().isEmpty())

        // Check both parties have the transaction.
        val aTx = a.services.validatedTransactions.getTransaction(settleTransaction.id)
        val bTx = b.services.validatedTransactions.getTransaction(settleTransaction.id)
        assertEquals(aTx, bTx)
    }

    @org.junit.Test
    fun `Partially settle non-anonymous obligation with non-anonymous cash payment`() {
        // Self issue cash.
        selfIssueCash(a, 1500.POUNDS)
        network.waitQuiescent()

        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        val amountToSettle = 500.POUNDS
        val settleTransaction = settleObligation(issuedObligation.linearId, a, amountToSettle, anonymous = false)
        network.waitQuiescent()
        assertEquals(1, settleTransaction.tx.outputsOfType<Obligation>().size)

        // Check both parties have the transaction.
        val aTx = a.services.validatedTransactions.getTransaction(settleTransaction.id)
        val bTx = b.services.validatedTransactions.getTransaction(settleTransaction.id)
        assertEquals(aTx, bTx)

        // Check the obligation paid amount is correctly updated.
        val partiallySettledObligation = settleTransaction.tx.outputsOfType<Obligation>().single()
        assertEquals(amountToSettle, partiallySettledObligation.paid)

        // Check cash has gone to the correct parties.
        val outputCash = settleTransaction.tx.outputsOfType<Cash.State>()
        assertEquals(2, outputCash.size)       // Cash to b and change to a.

        // Change addresses are always anonymous, I think.
        val change = getCashOutputByOwner(outputCash, a)
        assertEquals(1000.POUNDS, change.amount.withoutIssuer())

        val payment = getCashOutputByOwner(outputCash, b)
        assertEquals(500.POUNDS, payment.amount.withoutIssuer())
    }

    @org.junit.Test
    fun `Partially settle non-anonymous obligation with anonymous cash payment`() {
        // Self issue cash.
        selfIssueCash(a, 1500.POUNDS)
        network.waitQuiescent()

        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        val amountToSettle = 500.POUNDS
        val settleTransaction = settleObligation(issuedObligation.linearId, a, amountToSettle)
        network.waitQuiescent()
        assert(settleTransaction.tx.outputsOfType<Obligation>().size == 1)

        // Check both parties have the transaction.
        val aTx = a.services.validatedTransactions.getTransaction(settleTransaction.id)
        val bTx = b.services.validatedTransactions.getTransaction(settleTransaction.id)
        assertEquals(aTx, bTx)

        // Check the obligation paid amount is correctly updated.
        val partiallySettledObligation = settleTransaction.tx.outputsOfType<Obligation>().single()
        assertEquals(amountToSettle, partiallySettledObligation.paid)

        // Check cash has gone to the correct parties.
        val outputCash = settleTransaction.tx.outputsOfType<Cash.State>()
        assertEquals(2, outputCash.size)       // Cash to b and change to a.

        val change = getCashOutputByOwner(outputCash, a)
        assertEquals(1000.POUNDS, change.amount.withoutIssuer())

        val payment = getCashOutputByOwner(outputCash, b)
        assertEquals(500.POUNDS, payment.amount.withoutIssuer())
    }

    @org.junit.Test
    fun `Partially settle anonymous obligation with anonymous cash payment`() {
        // Self issue cash.
        selfIssueCash(a, 1500.POUNDS)
        network.waitQuiescent()

        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Attempt settlement.
        val amountToSettle = 500.POUNDS
        val settleTransaction = settleObligation(issuedObligation.linearId, a, amountToSettle)
        network.waitQuiescent()
        assertEquals(1, settleTransaction.tx.outputsOfType<Obligation>().size)

        // Check both parties have the transaction.
        val aTx = a.services.validatedTransactions.getTransaction(settleTransaction.id)
        val bTx = b.services.validatedTransactions.getTransaction(settleTransaction.id)
        assertEquals(aTx, bTx)

        // Check the obligation paid amount is correctly updated.
        val partiallySettledObligation = settleTransaction.tx.outputsOfType<Obligation>().single()
        assertEquals(amountToSettle, partiallySettledObligation.paid)

        // Check cash has gone to the correct parties.
        val outputCash = settleTransaction.tx.outputsOfType<Cash.State>()
        assertEquals(2, outputCash.size)       // Cash to b and change to a.

        val change = getCashOutputByOwner(outputCash, a)
        assertEquals(1000.POUNDS, change.amount.withoutIssuer())

        val payment = getCashOutputByOwner(outputCash, b)
        assertEquals(500.POUNDS, payment.amount.withoutIssuer())
    }

}
