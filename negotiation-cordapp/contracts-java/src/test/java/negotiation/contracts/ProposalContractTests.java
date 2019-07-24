package negotiation.contracts;

import com.google.common.collect.ImmutableList;
import negotiation.states.ProposalState;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;


import java.time.Instant;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ProposalContractTests {

    private MockServices ledgerServices = new MockServices(ImmutableList.of("negotiation.contracts"));
    private TestIdentity alice = new TestIdentity(new net.corda.core.identity.CordaX500Name("alice","New York", "US"));
    private TestIdentity bob = new TestIdentity(new net.corda.core.identity.CordaX500Name("bob","Tokyo", "JP"));
    private TestIdentity charlie = new TestIdentity(new net.corda.core.identity.CordaX500Name("charlie","London", "GB"));

    @Test
    public void proposalTransactionsHaveOnlyOneOutputOfTypeProposalState(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                tx.tweak(tdsl->{
                    tdsl.output(ProposalAndTradeContract.ID,new DummyState());
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl -> {
                    tdsl.output(ProposalAndTradeContract.ID, new ProposalState(2,alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.output(ProposalAndTradeContract.ID, new ProposalState(2,alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.output(ProposalAndTradeContract.ID, new ProposalState(2,alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void proposalTransactionsHaveExactlyOneCommandOfTypePropose(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.output(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.tweak(tdsl -> {
                    tdsl.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()),  DummyCommandData.INSTANCE);
                    tdsl.fails();
                    return null;
                });
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                tx.verifies();
                return null;
            });
            return null;
        });
    }



    @Test
    public void proposalTransactionsHaveTwoRequiredSignersTheProposerAndProposee(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.output(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.tweak(tdsl ->{
                    tdsl.command(ImmutableList.of(alice.getPublicKey(), charlie.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl -> {
                    tdsl.command(ImmutableList.of(charlie.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                    tdsl.fails();
                    return null;
                });
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void inProposalTransactionsProposerAndProposeeAreBuyerAndSeller(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                tx.tweak(tdsl ->{
                    //order reversed - buyer = proposee, seller = proposer
                    tdsl.output(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), bob.getParty(), bob.getParty(), alice.getParty()));
                    tdsl.verifies();
                    return null;
                });
                tx.tweak(tdsl -> {
                    tdsl.output(ProposalAndTradeContract.ID, new ProposalState(2, charlie.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl -> {
                    tdsl.output(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), charlie.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.output(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        });
    }



    @Test
    public void proposalAcceptanceTransactionsHaveNoInputsAndNoTimeStamp(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.output(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Propose());
                tx.tweak(tdsl ->{
                    tdsl.input(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl ->{
                    tdsl.timeWindow(Instant.now());
                    tdsl.fails();
                    return null;
                });
                return null;
            });
            return null;
        });
    }

}
