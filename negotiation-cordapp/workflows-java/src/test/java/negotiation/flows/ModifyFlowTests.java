package negotiation.flows;

import com.google.common.collect.ImmutableList;
import negotiation.states.ProposalState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ModifyFlowTests extends FlowTestsBase {

    @Test
    public void modificationFlowConsumesTheProposalsInBothNodesVaultsAndReplacesWithEquivWithNEwAmountsWhenInitiatorISBuyer() throws ExecutionException, InterruptedException {
        testModification(true);
    }

    @Test
    public void modificationFlowConsumesTheProposalsInBothNodesVaultsAndReplacesWithEquivWithNEwAmountsWhenInitiatorIsNotBuyer() throws ExecutionException, InterruptedException {
        testModification(false);
    }

    @Test(expected = ExecutionException.class)
    public void modificationFlowThrowsAnErrorIfTheProposerTriesToModifyTheProposal() throws ExecutionException, InterruptedException {
        int oldAmount = 1;
        int newAmount = 2;
        Party counterparty = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        UniqueIdentifier proposalID = nodeACreatesProposal(true, oldAmount, counterparty);
        ModificationFlow.Initiator flow = new ModificationFlow.Initiator(proposalID, newAmount );
        Future future = a.startFlow(flow);
        network.runNetwork();
        future.get();

    }


    public void testModification(Boolean isBuyer) throws ExecutionException, InterruptedException {
        int oldAmount = 1;
        int newAmount = 2;
        Party counterparty = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        UniqueIdentifier proposalID = nodeACreatesProposal(isBuyer, oldAmount, counterparty);
        nodeBModifiesProposal(proposalID,newAmount);

        ImmutableList.of(a,b).forEach(node -> {
            node.transaction(() -> {
                List<StateAndRef<ProposalState>> proposals = node.getServices().getVaultService().queryBy(ProposalState.class).getStates();
                Assert.assertEquals(1, proposals.size());

                ProposalState proposal = proposals.get(0).getState().getData();

                Assert.assertEquals(newAmount, proposal.getAmount());
                Party buyer;
                Party seller;
                Party proposer;
                Party proposee;

                if(isBuyer){
                    buyer = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    proposer = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    seller = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    proposee = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                }else{
                    seller = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    proposer = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    buyer = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                    proposee = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
                }

                Assert.assertEquals(buyer, proposal.getBuyer());
                Assert.assertEquals(seller, proposal.getSeller());
                Assert.assertEquals(proposer, proposal.getProposer());
                Assert.assertEquals(proposee, proposal.getProposee());
                return null;
            });
            return;
        });
    }
}
