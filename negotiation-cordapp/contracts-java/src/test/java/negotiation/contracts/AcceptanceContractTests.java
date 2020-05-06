package negotiation.contracts;

import com.google.common.collect.ImmutableList;
import negotiation.states.ProposalState;
import negotiation.states.TradeState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.time.Instant;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class AcceptanceContractTests {

    private MockServices ledgerServices = new MockServices(ImmutableList.of("negotiation.contracts"));
    private TestIdentity alice = new TestIdentity(new net.corda.core.identity.CordaX500Name("alice","New York", "US"));
    private TestIdentity bob = new TestIdentity(new net.corda.core.identity.CordaX500Name("bob","Tokyo", "JP"));
    private TestIdentity charlie = new TestIdentity(new net.corda.core.identity.CordaX500Name("charlie","London", "GB"));

    @Test
    public void proposalAcceptanceTransactionsHaveOnlyOneInputAndOneOutputState(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                tx.tweak(tdsl->{
                    tdsl.input(ProposalAndTradeContract.ID, new ProposalState(1,alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.input(ProposalAndTradeContract.ID, new ProposalState(1,alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl -> {
                    tdsl.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                    tdsl.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.input(ProposalAndTradeContract.ID,new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1,alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void proposalAcceptanceTransactionsHaveInputOfTypeProposalStateAndOutputOfTypeTradeState(){
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                tx.tweak(tdsl ->{
                    tdsl.input(ProposalAndTradeContract.ID,new TradeState(1,alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl ->{
                    tdsl.output(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(),bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });

                tx.input(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void proposalAcceptanceTransactionsHaveExactlyOneCommandOfTypeAccept(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.input(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                tx.tweak(tdsl -> {
                    tdsl.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()),  DummyCommandData.INSTANCE);
                    tdsl.fails();
                    return null;
                });
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void inputProposalStateAndOutputTradeStateShouldHaveExactlyTheSameAmount(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                tx.tweak(tdsl ->{
                    tdsl.input(ProposalAndTradeContract.ID,new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.output(ProposalAndTradeContract.ID, new TradeState(2, alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl ->{
                    tdsl.input(ProposalAndTradeContract.ID, new ProposalState(2, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                    tdsl.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.input(ProposalAndTradeContract.ID,new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        });
    }


    @Test
    public void buyerAndSellerAreUnmodifiedInTheOutput(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.input(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                tx.tweak(tdsl ->{
                    tdsl.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(),  charlie.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl ->{
                    tdsl.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(),  charlie.getParty()));
                    tdsl.fails();
                    return null;
                });
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void proposalAcceptanceTransactionsHaveTwoRequiredSignersTheProposerAndProposee(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.input(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                tx.tweak(tdsl ->{
                    tdsl.command(ImmutableList.of(alice.getPublicKey(), charlie.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                    tdsl.fails();
                    return null;
                });
                tx.tweak(tdsl -> {
                    tdsl.command(ImmutableList.of(charlie.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                    tdsl.fails();
                    return null;
                });
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void proposalAcceptanceTransactionsHaveNoTimeStamp(){
        ledger(ledgerServices, ledger ->{
            ledger.transaction(tx ->{
                tx.input(ProposalAndTradeContract.ID, new ProposalState(1, alice.getParty(), bob.getParty(), alice.getParty(), bob.getParty()));
                tx.output(ProposalAndTradeContract.ID, new TradeState(1, alice.getParty(), bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new ProposalAndTradeContract.Commands.Accept());
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
