package negotiation.flows;

import com.google.common.collect.ImmutableList;
import negotiation.states.ProposalState;
import negotiation.states.TradeState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AcceptanceFlowTests extends FlowTestsBase{

    @Test
    public void acceptanceFlowConsumesTheProposalsInBothNodesVaultsAndReplacesWithEquivAccTradesWhenInitiatorIsBuyer() throws ExecutionException, InterruptedException {
        testAcceptance(true);

    }

    @Test
    public void acceptanceFlowConsumesTheProposalsInBothNodesVaultsAndReplacesWithEquivAccTradesWhenInitiatorIsSeller() throws ExecutionException, InterruptedException {
        testAcceptance(false);
    }

    @Test(expected = ExecutionException.class)
    public void acceptanceFlowThrowsAnErrorIfTheProposerTriesToAcceptTheProposal() throws ExecutionException, InterruptedException {
        int amount = 1;
        Party counterparty = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        UniqueIdentifier proposalID = nodeACreatesProposal(true, amount, counterparty);

        AcceptanceFlow.Initiator flow = new AcceptanceFlow.Initiator(proposalID);
        Future future = a.startFlow(flow);
        network.runNetwork();
        future.get();
    }

    private void testAcceptance(Boolean isBuyer) throws ExecutionException, InterruptedException {
        int amount = 1;
        Party counterparty = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        UniqueIdentifier proposalId = nodeACreatesProposal(isBuyer, amount, counterparty);
        nodeBAcceptsProposal(proposalId);
        ImmutableList.of(a,b).forEach(node -> {
            node.transaction(() -> {
                List<StateAndRef<ProposalState>> proposals = node.getServices().getVaultService().queryBy(ProposalState.class).getStates();
                Assert.assertEquals(0, proposals.size());

                List<StateAndRef<TradeState>> trades = node.getServices().getVaultService().queryBy(TradeState.class).getStates();
                Assert.assertEquals(1, trades.size());
                TradeState trade = trades.get(0).getState().getData();

                Assert.assertEquals(amount, trade.getAmount());
                Party buyer;
                Party seller;

                if(isBuyer){
                    buyer = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    seller = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                }else{
                    seller = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    buyer = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                }

                Assert.assertEquals(buyer, trade.getBuyer());
                Assert.assertEquals(seller, trade.getSeller());
                return null;
            });
            return;
        });
    }
}
