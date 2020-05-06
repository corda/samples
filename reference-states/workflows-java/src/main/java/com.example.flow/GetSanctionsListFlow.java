package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.state.SanctionedEntities;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;

import java.util.Collections;
import java.util.List;

public class GetSanctionsListFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<List<StateAndRef<SanctionedEntities>>>{
        Party otherParty;

        public Initiator(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Suspendable
        @Override
        public List<StateAndRef<SanctionedEntities>> call() throws FlowException {
            FlowSession session = initiateFlow(otherParty);
            String resolve = session.receive(String.class).unwrap(it -> it);

            if (resolve.equals("YES")){
                SignedTransaction newestSanctionList = subFlow(new ReceiveTransactionFlow(session,true, StatesToRecord.ALL_VISIBLE));
                return newestSanctionList.getCoreTransaction().outRefsOfType(SanctionedEntities.class);
            }else{
                return Collections.emptyList();
            }
        }
    }


    @InitiatedBy(Initiator.class)
    public static class Acceptor extends  FlowLogic<Void>{
        private FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            List<StateAndRef<SanctionedEntities>> states = getServiceHub().getVaultService().queryBy(SanctionedEntities.class).getStates();
            if(states.isEmpty()){
                otherPartySession.send("NO");
            }else{
                otherPartySession.send("YES");
                subFlow(
                        new SendTransactionFlow(
                                otherPartySession,
                                getServiceHub().getValidatedTransactions().getTransaction(states.get(0).getRef().getTxhash())
                        )
                );
            }

            return null;
        }
    }

}
