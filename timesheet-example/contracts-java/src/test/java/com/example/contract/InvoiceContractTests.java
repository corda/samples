package com.example.contract;

import com.google.common.collect.ImmutableList;
import com.example.state.InvoiceState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;


public class InvoiceContractTests {
    private MockServices ledgerServices = new MockServices(ImmutableList.of("com.example.contract"));
    private TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    private TestIdentity contractor  =new TestIdentity(new CordaX500Name("Contractor", "New York", "US"));
    private TestIdentity oracle = new TestIdentity(new CordaX500Name("Oracle", "New York", "US"));
    private List<PublicKey> signers = ImmutableList.of(megaCorp.getPublicKey(), contractor.getPublicKey(), oracle.getPublicKey());
    private LocalDate date = LocalDate.now();
    private Double rate = 10.0;
    private int invoiceValue = 1;

    @Test
    public void transactionMustIncludeCreateCommand(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                //tx.fails();
                tx.command(signers, new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(InvoiceContract.ID, new InvoiceState(date, invoiceValue,rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.command(signers, new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.failsWith("No inputs should be consumed when issuing an invoice.");
                return null;
            });
            return null;
        }));
    }


    @Test
    public void transactionMustHaveOneOutput(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.command(signers, new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void contractorMustSignTransaction(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(),oracle.getPublicKey()), new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void companyMustSignTransaction(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.command(ImmutableList.of(contractor.getPublicKey(),oracle.getPublicKey()), new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void contractorIsNotCompany(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(InvoiceContract.ID, new InvoiceState(date, invoiceValue, rate, megaCorp.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.command(signers, new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.failsWith("The lender and the borrower cannot be the same entity.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotCreateNegativeValueInvoices(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(InvoiceContract.ID, new InvoiceState(date, -1, rate, contractor.getParty(), megaCorp.getParty(), oracle.getParty(), false));
                tx.command(signers, new InvoiceContract.Commands.Create(contractor.getParty(), megaCorp.getParty(), rate));
                tx.failsWith("The Invoice's value must be non-negative.");
                return null;
            });
            return null;
        }));
    }
}
