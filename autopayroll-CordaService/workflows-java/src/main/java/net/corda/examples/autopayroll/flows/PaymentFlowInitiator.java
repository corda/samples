package net.corda.examples.autopayroll.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.autopayroll.contracts.MoneyStateContract;
import net.corda.examples.autopayroll.states.MoneyState;
import net.corda.examples.autopayroll.states.PaymentRequestState;

import java.util.List;

@InitiatingFlow
@StartableByService
public class PaymentFlowInitiator extends FlowLogic<SignedTransaction> {

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        // Initiator flow logic goes here
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        List<StateAndRef<PaymentRequestState>> wBStateList = getServiceHub().getVaultService().queryBy(PaymentRequestState.class).getStates();
        PaymentRequestState vaultState = wBStateList.get(wBStateList.size() - 1).getState().getData();
        MoneyState output = new MoneyState((Integer.getInteger(vaultState.getAmount())), vaultState.getTowhom());

        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        CommandData commandData = new MoneyStateContract.Commands.Pay();
        txBuilder.addCommand(commandData, getOurIdentity().getOwningKey(), vaultState.getTowhom().getOwningKey());
        txBuilder.addOutputState(output, MoneyStateContract.ID);
        txBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(vaultState.getTowhom());
        SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, ImmutableList.of(session)));

        return subFlow(new FinalityFlow(stx, ImmutableList.of(session)));
    }
}