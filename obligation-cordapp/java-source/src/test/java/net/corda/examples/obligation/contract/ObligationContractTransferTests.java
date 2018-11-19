package net.corda.examples.obligation.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.examples.obligation.Obligation;
import net.corda.examples.obligation.ObligationContract;
import net.corda.testing.contracts.DummyContract;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static net.corda.examples.obligation.ObligationContract.OBLIGATION_CONTRACT_ID;
import static net.corda.finance.Currencies.DOLLARS;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ObligationContractTransferTests extends ObligationContractUnitTests {

    @Test
    public void mustHandleMultipleCommandValues() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new DummyCommand());
                tx.failsWith("Required net.corda.examples.obligation.ObligationContract.Commands command");
                return null;
            });
            ledger.transaction(tx -> {
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustHaveOneInputAndOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.input(DummyContract.PROGRAM_ID, new DummyState());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("An obligation transfer transaction should only consume one input state.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("An obligation transfer transaction should only consume one input state.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("An obligation transfer transaction should only create one output state.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.output(OBLIGATION_CONTRACT_ID, new DummyState());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("An obligation transfer transaction should only create one output state.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void onlyTheLenderMayChange() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), alice.getParty(), bob.getParty()));
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(1), alice.getParty(), bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("Only the lender property may change.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), alice.getParty(), bob.getParty()));
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), alice.getParty(), charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("Only the lender property may change.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), alice.getParty(), bob.getParty(), DOLLARS(5)));
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), alice.getParty(), bob.getParty(), DOLLARS(10)));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("Only the lender property may change.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void theLenderMustChange() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("The lender property must change in a transfer.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void allParticipantsMustSign() {
        TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "", "GB"));

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("The borrower, old lender and new lender only must sign an obligation transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("The borrower, old lender and new lender only must sign an obligation transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("The borrower, old lender and new lender only must sign an obligation transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), miniCorp.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("The borrower, old lender and new lender only must sign an obligation transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey(), miniCorp.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.failsWith("The borrower, old lender and new lender only must sign an obligation transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation.withNewLender(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Transfer());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }
}
