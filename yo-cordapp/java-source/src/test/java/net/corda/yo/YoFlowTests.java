package net.corda.yo;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;


public class YoFlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup(){
        network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(TestCordapp.findCordapp("net.corda.yo"))));
        a = network.createNode();
        b = network.createNode();
        network.runNetwork();
    }


    @After
    public void tearDown(){
        network.stopNodes();
    }
    @Test
    public void flowWorksCorrectly() throws ExecutionException, InterruptedException {
        a.getServices().getMyInfo().getLegalIdentities().get(0);
        YoState yo = new YoState(a.getServices().getMyInfo().getLegalIdentities().get(0), b.getServices().getMyInfo().getLegalIdentities().get(0));
        YoFlow flow = new YoFlow(b.getServices().getMyInfo().getLegalIdentities().get(0));
        CordaFuture future =  a.startFlow(flow);
        network.runNetwork();
        network.waitQuiescent();
        Object stx = future.get();
        if(stx instanceof SignedTransaction){
            stx = (SignedTransaction) stx;
            SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction(((SignedTransaction) stx).getId());
            assertEquals(bTx,stx);
            System.out.println(bTx.toString() + " == " + stx.toString());
        }
        YoState retState = b.getServices().getVaultService().queryBy(YoState.class).getStates().get(0).getState().getData();
        assertEquals(retState.toString(), yo.toString());
        System.out.println(retState.toString() + " == " + yo.toString());
    }

}


/*
class YoContractTests {
    private val ledgerServices = MockServices(listOf("net.corda.yo", "net.corda.testing.contracts"))
    private val alice = TestIdentity(CordaX500Name("Alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("Bob", "Tokyo", "JP"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun yoTransactionMustBeWellFormed() {
        // A pre-made Yo to Bob.
        val yo = YoState(alice.party, bob.party)
        // YoFlowTests.
        ledgerServices.ledger {
            // Input state present.
            transaction {
                input(DummyContract.PROGRAM_ID, DummyState())
                command(alice.publicKey, YoContract.Send())
                output(YoContract.ID, yo)
                this.failsWith("There can be no inputs when Yo'ing other parties.")
            }
            // Wrong command.
            transaction {
                output(YoContract.ID, yo)
                command(alice.publicKey, DummyCommandData)
                this.failsWith("")
            }
            // Command signed by wrong key.
            transaction {
                output(YoContract.ID, yo)
                command(miniCorp.publicKey, YoContract.Send())
                this.failsWith("The Yo! must be signed by the sender.")
            }
            // Sending to yourself is not allowed.
            transaction {
                output(YoContract.ID, YoState(alice.party, alice.party))
                command(alice.publicKey, YoContract.Send())
                this.failsWith("No sending Yo's to yourself!")
            }
            transaction {
                output(YoContract.ID, yo)
                command(alice.publicKey, YoContract.Send())
                this.verifies()
            }
        }
    }
}
 */