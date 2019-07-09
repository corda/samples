package net.corda.yo;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyContract;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;


import static net.corda.testing.node.NodeTestUtils.ledger;

public class YoContractTests {
    MockServices ledgerServices = new MockServices(ImmutableList.of("net.corda.yo", "net.corda.testing.contracts"));
    TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "New York", "US"));
    TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "Tokyo", "JP"));
    TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "New York", "US"));


    @Test
    public void yoTransactionMustBeWellFormed() {
        YoState yo = new YoState(alice.getParty(), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            //Too many inputs
            ledger.transaction(tx -> {
                tx.input(DummyContract.PROGRAM_ID, new DummyState());
                tx.command(alice.getPublicKey(), new YoContract.Commands.Send());
                tx.output(YoContract.ID, yo);
                tx.failsWith("There can be no inputs when Yo'ing other parties");
                return null;
            });
            //Wrong Command
            ledger.transaction(tx -> {
                tx.output(YoContract.ID, yo);
                tx.command(alice.getPublicKey(), new DummyCommand());
                tx.failsWith("");
                return null;
            });
            // Command signed by wrong key
            ledger.transaction(tx -> {
               tx.output(YoContract.ID,yo);
               tx.command(miniCorp.getPublicKey(), new YoContract.Commands.Send());
               tx.failsWith("The Yo! must be signed by the sender.");
               return null;
            });

            //Sending to yourself is not allowed
            ledger.transaction(tx -> {
                tx.output(YoContract.ID, new YoState(alice.getParty(), alice.getParty()));
                tx.command(miniCorp.getPublicKey(), new YoContract.Commands.Send());
                tx.failsWith("No sending Yo's to yourself!");
                return null;
            });

            //You can do a transaction
            ledger.transaction(tx -> {
                tx.output(YoContract.ID, yo);
                tx.command(alice.getPublicKey(), new YoContract.Commands.Send());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }
};


class DummyCommand implements CommandData {}