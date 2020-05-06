package net.corda.examples.obligation.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.examples.obligation.Obligation;
import net.corda.examples.obligation.ObligationContract;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import java.util.Currency;

import static net.corda.examples.obligation.ObligationContract.OBLIGATION_CONTRACT_ID;
import static net.corda.finance.Currencies.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ObligationContractSettleTests extends ObligationContractUnitTests {
    private TestIdentity issuer = new TestIdentity(new CordaX500Name("MegaBank", "", "US"));
    private Byte defaultRef = Byte.MAX_VALUE;
    private PartyAndReference defaultIssuer = issuer.ref(defaultRef);

    private Cash.State createCashState(Amount<Currency> amount, AbstractParty owner) {
        return new Cash.State(issuedBy(amount, defaultIssuer), owner);
    }

    @Test
    public void mustIncludeSettleCommand() {
        Cash.State inputCash = createCashState(DOLLARS(5), bob.getParty());
        Cash.State outputCash = (Cash.State) inputCash.withNewOwner(alice.getParty()).getOwnableState();
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.input(Cash.PROGRAM_ID, inputCash);
                tx.output(Cash.PROGRAM_ID, outputCash);
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.fails();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.input(Cash.PROGRAM_ID, inputCash);
                tx.output(Cash.PROGRAM_ID, outputCash);
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new DummyCommand()); // Wrong type.
                tx.fails();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.input(Cash.PROGRAM_ID, inputCash);
                tx.output(Cash.PROGRAM_ID, outputCash);
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle()); // Correct Type.
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustHaveOnlyOneInputObligation() {
        Obligation duplicateObligation = new Obligation(DOLLARS(10), alice.getParty(), bob.getParty());
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.failsWith("There must be one input obligation.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(OBLIGATION_CONTRACT_ID, duplicateObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.failsWith("There must be one input obligation.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.input(Cash.PROGRAM_ID, tenDollars);
                tx.output(Cash.PROGRAM_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustBeCashOutputStatesPresent() {
        Cash.State cash = createCashState(DOLLARS(5), bob.getParty());
        CommandAndState cashPayment = cash.withNewOwner(alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("There must be output cash.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, cash);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.output(Cash.PROGRAM_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustBeCashOutputStatesWithRecipientAsOwner() {
        Cash.State cash = createCashState(DOLLARS(5), bob.getParty());
        CommandAndState invalidCashPayment = cash.withNewOwner(charlie.getParty());
        CommandAndState validCashPayment = cash.withNewOwner(alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, cash);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.output(Cash.PROGRAM_ID, invalidCashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), invalidCashPayment.getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("There must be output cash paid to the recipient.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, cash);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.output(Cash.PROGRAM_ID, validCashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), validCashPayment.getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cashSettlementAmountMustBeLessThanTheRemainingAmount() {
        Cash.State elevenDollars = createCashState(DOLLARS(11), bob.getParty());
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, elevenDollars);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(11)));
                tx.output(Cash.PROGRAM_ID, elevenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), elevenDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("The amount settled cannot be more than the amount outstanding.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, tenDollars);
                tx.output(Cash.PROGRAM_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cashSettlementMustBeInTheCorrectCurrency() {
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State tenPounds = createCashState(POUNDS(10), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, tenPounds);
                tx.output(Cash.PROGRAM_ID, tenPounds.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenPounds.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("Token mismatch: GBP vs USD");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, tenDollars);
                tx.output(Cash.PROGRAM_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustHaveOutputObligationIfNotFullySettling() {
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("There must be one output obligation.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(Cash.PROGRAM_ID, tenDollars);
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(10)));
                tx.output(Cash.PROGRAM_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("There must be no output obligation as it has been fully settled.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(Cash.PROGRAM_ID, tenDollars);
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(Cash.PROGRAM_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void onlyPaidPropertyMayChange() {
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(tenDollarObligation.getAmount(), tenDollarObligation.getLender(), alice.getParty(), DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("The borrower may not change when settling.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(0), tenDollarObligation.getLender(), tenDollarObligation.getBorrower(), DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("The amount may not change when settling.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(OBLIGATION_CONTRACT_ID, new Obligation(DOLLARS(10), charlie.getParty(), tenDollarObligation.getBorrower(), DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("The lender may not change when settling.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.input(Cash.PROGRAM_ID, fiveDollars);
                tx.output(Cash.PROGRAM_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustBeSignedByAllParticipants() {
        Cash.State cash = createCashState(DOLLARS(5), bob.getParty());
        CommandAndState cashPayment = cash.withNewOwner(alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(Cash.PROGRAM_ID, cash);
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(Cash.PROGRAM_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.command(ImmutableList.of(alice.getPublicKey(), charlie.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign obligation settle transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(Cash.PROGRAM_ID, cash);
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(Cash.PROGRAM_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.command(bob.getPublicKey(), new ObligationContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign obligation settle transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(Cash.PROGRAM_ID, cash);
                tx.input(OBLIGATION_CONTRACT_ID, tenDollarObligation);
                tx.output(Cash.PROGRAM_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.output(OBLIGATION_CONTRACT_ID, tenDollarObligation.pay(DOLLARS(5)));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ObligationContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }
}