package negotiation.flows;

import com.google.common.collect.ImmutableList;
import negotiation.states.ProposalState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ProposalFlowTests extends FlowTestsBase {

    @Test
    public void proposalFlowCreatesTheCorrectProposalsInBothNodesVaultsWhenInitiatorIsBuyer() throws ExecutionException, InterruptedException {
        testProposal(true);
    }

    @Test
    public void proposalFlowCreatesTheCorrectProposalsInBothNodesVaultsWhenInitiatorIsSeller() throws ExecutionException, InterruptedException {
        testProposal(false);
    }


    private void testProposal(Boolean isBuyer) throws ExecutionException, InterruptedException {
        int amount = 1;
        Party counterparty = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        nodeACreatesProposal(isBuyer, amount, counterparty);
        ImmutableList.of(a,b).forEach(node -> {
            node.transaction(() -> {
                List<StateAndRef<ProposalState>> proposals = node.getServices().getVaultService().queryBy(ProposalState.class).getStates();
                Assert.assertEquals(1, proposals.size());

                ProposalState proposal = proposals.get(0).getState().getData();

                Assert.assertEquals(amount, proposal.getAmount());
                Party buyer;
                Party seller;

                if(isBuyer){
                    buyer = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    seller = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                }else{
                    seller = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    buyer = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                }

                Assert.assertEquals(buyer, proposal.getBuyer());
                Assert.assertEquals(seller, proposal.getSeller());
                return null;
            });
            return;
        });
    }
}
