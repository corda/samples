package net.corda.examples.oracle.service;

import com.google.common.collect.ImmutableList;
import kotlin.jvm.JvmField;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.core.transactions.MissingContractAttachments;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.oracle.base.contract.PrimeContract;
import net.corda.examples.oracle.base.contract.PrimeState;
import net.corda.examples.oracle.service.service.Oracle;
import net.corda.testing.core.SerializationEnvironmentRule;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.InvalidKeyException;
import java.security.SignatureException;

public class PrimeServiceTests {
    private final TestIdentity oracleIdentity = new TestIdentity(new CordaX500Name("Oracle", "New York", "US"));
    private final MockServices dummyServices = new MockServices((Iterable<String>) ImmutableList.of("net.corda.examples.oracle.base.contract"), oracleIdentity);
    private final Oracle oracle = new Oracle(dummyServices);
    private final TestIdentity aliceIdentity = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity notaryIdentity = new TestIdentity(new CordaX500Name("Notary", "", "GB"));

    @Rule
    @JvmField
    public final SerializationEnvironmentRule testSerialization = new SerializationEnvironmentRule();

    @Test
    public void oracleReturnsCorrectNthPrime() {
        Assert.assertEquals(104729, oracle.query(10000).longValue());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void oracleRejectsInvalidNValues() throws IllegalArgumentException {
        thrown.expect(IsInstanceOf.<Throwable>instanceOf(IllegalArgumentException.class));

        try {
            oracle.query(0);
        } catch (IllegalArgumentException e) {
            oracle.query(-1);
        }
    }

    @Test
    public void oracleSignsTXIncludingAValidPrime() throws MissingContractAttachments, FilteredTransactionVerificationException, SignatureException, InvalidKeyException {
        Command command = new Command(new PrimeContract.Commands.Create(10, 29), ImmutableList.of(oracleIdentity.getPublicKey()));
        PrimeState state = new PrimeState(10, 29, aliceIdentity.getParty());
        StateAndContract stateAndContract = new StateAndContract(state, PrimeContract.PRIME_PROGRAM_ID);
        FilteredTransaction ftx = new TransactionBuilder(notaryIdentity.getParty())
                .withItems(stateAndContract, command)
                .toWireTransaction(dummyServices)
                .buildFilteredTransaction(o -> {
                    if (o instanceof Command && ((Command) o).getSigners().contains(oracleIdentity.getParty().getOwningKey())
                            && ((Command) o).getValue() instanceof PrimeContract.Commands.Create) {
                        return  true;
                    }
                    return false;
                });
        TransactionSignature signature = oracle.sign(ftx);
        assert (signature.verify(ftx.getId()));
    }

    @Test
    public void oracleDoesNotSignTransactionsIncludingAnInvalidPrime() throws MissingContractAttachments, FilteredTransactionVerificationException {
        thrown.expect(IsInstanceOf.<Throwable>instanceOf(IllegalArgumentException.class));

        Command command = new Command(new PrimeContract.Commands.Create(10, 1000), ImmutableList.of(oracleIdentity.getPublicKey()));
        PrimeState state = new PrimeState(10, 29, aliceIdentity.getParty());
        StateAndContract stateAndContract = new StateAndContract(state, PrimeContract.PRIME_PROGRAM_ID);
        FilteredTransaction ftx = new TransactionBuilder(notaryIdentity.getParty())
                .withItems(stateAndContract, command)
                .toWireTransaction(dummyServices)
                .buildFilteredTransaction(o -> {
                    if (o instanceof Command && ((Command) o).getSigners().contains(oracleIdentity.getParty().getOwningKey())
                            && ((Command) o).getValue() instanceof PrimeContract.Commands.Create) {
                        return  true;
                    }
                    return false;
                });
        oracle.sign(ftx);
    }

}
