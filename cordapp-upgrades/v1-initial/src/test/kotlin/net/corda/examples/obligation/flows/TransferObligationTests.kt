package net.corda.examples.obligation.flows

import net.corda.examples.obligation.Obligation
import net.corda.finance.POUNDS
import net.corda.testing.internal.chooseIdentity
import org.jgroups.util.Util.assertEquals

class TransferObligationTests : ObligationTests() {

    @org.junit.Test
    fun `Transfer non-anonymous obligation successfully`() {
        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Transfer obligation.
        val transferTransaction = transferObligation(issuedObligation.linearId, b, c, anonymous = false)
        network.waitQuiescent()
        val transferredObligation = transferTransaction.tx.outputStates.first() as Obligation

        // Check the issued obligation with the new lender is the transferred obligation
        assertEquals(issuedObligation.withNewLender(c.info.chooseIdentity()), transferredObligation)

        // Check everyone has the transfer transaction.
        val aObligation = a.services.loadState(transferTransaction.tx.outRef<Obligation>(0).ref).data as Obligation
        val bObligation = b.services.loadState(transferTransaction.tx.outRef<Obligation>(0).ref).data as Obligation
        val cObligation = c.services.loadState(transferTransaction.tx.outRef<Obligation>(0).ref).data as Obligation
        assertEquals(aObligation, bObligation)
        assertEquals(bObligation, cObligation)
    }

    @org.junit.Test
    fun `Transfer anonymous obligation successfully`() {
        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Transfer obligation.
        val transferTransaction = transferObligation(issuedObligation.linearId, b, c)
        network.waitQuiescent()
        val transferredObligation = transferTransaction.tx.outputStates.first() as Obligation

        // Check the issued obligation with the new lender is the transferred obligation.
        assertEquals(issuedObligation.withNewLender(transferredObligation.lender), transferredObligation)

        // Check everyone has the transfer transaction.
        val aObligation = a.services.loadState(transferTransaction.tx.outRef<Obligation>(0).ref).data as Obligation
        val bObligation = b.services.loadState(transferTransaction.tx.outRef<Obligation>(0).ref).data as Obligation
        val cObligation = c.services.loadState(transferTransaction.tx.outRef<Obligation>(0).ref).data as Obligation
        assertEquals(aObligation, bObligation)
        assertEquals(bObligation, cObligation)

    }

    @org.junit.Test
    fun `Transfer flow can only be started by lender`() {
        // Issue obligation.
        val issuanceTransaction = issueObligation(a, b, 1000.POUNDS, anonymous = false)
        network.waitQuiescent()
        val issuedObligation = issuanceTransaction.tx.outputStates.first() as Obligation

        // Transfer obligation.
        kotlin.test.assertFailsWith<IllegalStateException> {
            transferObligation(issuedObligation.linearId, a, c, anonymous = false)
        }
    }

}
