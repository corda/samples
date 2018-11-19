package net.corda.examples.obligation.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.examples.obligation.Obligation
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.testing.contracts.DummyContract
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService

/**
 * A base class to reduce the boilerplate when writing obligation contract tests.
 */
abstract class ObligationContractUnitTests {
    protected val ledgerServices = MockServices(
            listOf("net.corda.examples.obligation", "net.corda.testing.contracts"),
            identityService = makeTestIdentityService(),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")))
    protected val alice = TestIdentity(CordaX500Name("Alice", "", "GB"))
    protected val bob = TestIdentity(CordaX500Name("Bob", "", "GB"))
    protected val charlie = TestIdentity(CordaX500Name("Bob", "", "GB"))

    protected class DummyState : ContractState {
        override val participants: List<AbstractParty> get() = listOf()
    }

    protected class DummyCommand : CommandData

    protected val oneDollarObligation = Obligation(1.POUNDS, alice.party, bob.party)
    protected val tenDollarObligation = Obligation(10.DOLLARS, alice.party, bob.party)
}
