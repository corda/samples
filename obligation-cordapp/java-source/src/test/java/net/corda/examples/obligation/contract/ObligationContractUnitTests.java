package net.corda.examples.obligation.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.examples.obligation.Obligation;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;

import java.util.List;

import static net.corda.finance.Currencies.DOLLARS;
import static net.corda.finance.Currencies.POUNDS;

/**
 * A base class to reduce the boilerplate when writing obligation contract tests.
 */
abstract class ObligationContractUnitTests {
    protected MockServices ledgerServices = new MockServices(
            ImmutableList.of("net.corda.examples.obligation", "net.corda.testing.contracts", "net.corda.finance.contracts"));
    protected TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    protected TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    protected TestIdentity charlie = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    protected Obligation oneDollarObligation = new Obligation(POUNDS(1), alice.getParty(), bob.getParty());
    protected Obligation tenDollarObligation = new Obligation(DOLLARS(10), alice.getParty(), bob.getParty());
}

class DummyState implements ContractState {
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }
}

class DummyCommand implements CommandData {}