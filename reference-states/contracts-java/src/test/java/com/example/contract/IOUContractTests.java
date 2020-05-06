package com.example.contract;

import com.example.state.SanctionableIOUState;
import com.example.state.SanctionedEntities;
import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static com.example.contract.SanctionableIOUContract.IOU_CONTRACT_ID;
import static com.example.contract.SanctionedEntitiesContract.SANCTIONS_CONTRACT_ID;
import static java.util.Collections.*;
import static net.corda.testing.common.internal.ParametersUtilitiesKt.testNetworkParameters;
import static net.corda.testing.node.NodeTestUtils.ledger;

import java.time.Duration;
import java.time.Instant;

public class IOUContractTests {
    CordaX500Name DUMMY_NOTARY_NAME = new CordaX500Name("Notary Service", "Zurich", "CH");
    private Party DUMMY_NOTARY = new TestIdentity(DUMMY_NOTARY_NAME, 20).getParty();
    private TestIdentity issuer = new TestIdentity(new CordaX500Name("SanctionsIssuer", "London", "GB"));
    private TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    private TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "New York", "US"));
    private TestIdentity naughtyCorp = new TestIdentity(new CordaX500Name("NaughtyCorp", "New York", "US"));

    private MockNetworkParameters networkParameters = new MockNetworkParameters();


    private MockServices ledgerServices = new MockServices(
            megaCorp,
            testNetworkParameters(emptyList(),
                    4, Instant.now(),10484760,10484760 * 50, emptyMap(), 1, Duration.ofDays(30), emptyMap()  ));

    private SanctionedEntities sanctions = new SanctionedEntities(ImmutableList.of(naughtyCorp.getParty()), issuer.getParty());
    private int iouValue =1;

    @Test
    public void transactionMustIncludeReferenceSanctionsListCommand(){
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new SanctionableIOUContract.Commands.Create(issuer.getParty()));
                tx.fails();
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.verifies();
                return null;
            });
            return null;
        });
    };

    @Test
    public void shouldNotAllowSenderToBeSanctioned(){
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, naughtyCorp.getParty(), megaCorp.getParty()));
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.command(
                        ImmutableList.of(naughtyCorp.getPublicKey(), megaCorp.getPublicKey()),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("The lender O=NaughtyCorp, L=New York, C=US is a sanctioned entity");
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustIncludeCreateCommand(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.fails();
                tx.command(
                        ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveNoInputs(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.input(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.command(
                        ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("No inputs should be consumed when issuing an IOU.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveOneOutput(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));

                tx.command(
                        ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void lenderMustSignTransaction(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.command(
                        miniCorp.getPublicKey(),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void borrowerMustSignTransaction(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, miniCorp.getParty(), megaCorp.getParty()));
                tx.command(
                        megaCorp.getPublicKey(),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void lenderISNotBorrower(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(iouValue, megaCorp.getParty(), megaCorp.getParty()));
                tx.command(
                        megaCorp.getPublicKey(),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("The lender and the borrower cannot be the same entity.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotCreateNegativeValueIOUs(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx -> {
                tx.reference(SANCTIONS_CONTRACT_ID, sanctions);
                tx.output(IOU_CONTRACT_ID, new SanctionableIOUState(-1, miniCorp.getParty(), megaCorp.getParty()));
                tx.command(
                        ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()),
                        new SanctionableIOUContract.Commands.Create(issuer.getParty())
                );
                tx.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        });
    }
}