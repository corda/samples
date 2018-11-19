package net.corda.examples.obligation.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.examples.obligation.Obligation;
import net.corda.examples.obligation.ObligationContract;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static net.corda.examples.obligation.ObligationContract.OBLIGATION_CONTRACT_ID;
import static net.corda.finance.Currencies.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ObligationContractIssueTests extends ObligationContractUnitTests {

    @Test
    public void issueObligationTransactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, new DummyState());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("No inputs should be consumed when issuing an obligation.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.verifies(); // As there are no input states.
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOnlyOneOutputObligation() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation); // Two outputs fails.
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("Only one obligation state should be created when issuing an obligation.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation); // One output passes.
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotIssueZeroValueObligations() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(POUNDS(0), alice.getParty(), bob.getParty())); // Zero amount fails.
                tx.failsWith("A newly issued obligation must have a positive amount.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(SWISS_FRANCS(100), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(POUNDS(1), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderAndBorrowerMustSignIssueObligationTransaction() {
        TestIdentity dummyIdentity = new TestIdentity(new CordaX500Name("Dummy", "", "GB"));

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(dummyIdentity.getPublicKey(), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("Both lender and borrower together only may sign obligation issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(alice.getPublicKey(), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("Both lender and borrower together only may sign obligation issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(bob.getPublicKey(), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("Both lender and borrower together only may sign obligation issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bob.getPublicKey(), bob.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("Both lender and borrower together only may sign obligation issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bob.getPublicKey(), bob.getPublicKey(), dummyIdentity.getPublicKey(), alice.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.failsWith("Both lender and borrower together only may sign obligation issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bob.getPublicKey(), bob.getPublicKey(), bob.getPublicKey(), alice.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderAndBorrowerCannotBeTheSame() {
        Obligation borrowerIsLenderObligation = new Obligation(POUNDS(10), alice.getParty(), alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, borrowerIsLenderObligation);
                tx.failsWith("The lender and borrower cannot be the same identity.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Issue());
                tx.output(OBLIGATION_CONTRACT_ID, oneDollarObligation);
                tx.verifies();
                return null;
            });
            return null;
        }));
    }
}