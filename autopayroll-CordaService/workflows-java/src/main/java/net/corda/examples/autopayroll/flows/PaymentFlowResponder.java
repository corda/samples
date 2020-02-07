package net.corda.examples.autopayroll.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

@InitiatedBy(PaymentFlowInitiator.class)
public class PaymentFlowResponder extends FlowLogic<Void> {
    private final FlowSession counterPartySession;

    public PaymentFlowResponder(FlowSession counterPartySession) {
        this.counterPartySession = counterPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // responder flow logic goes here
        SignedTransaction stx = subFlow(new SignTransactionFlow(counterPartySession) {
            @Suspendable
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                if (counterPartySession.getCounterparty().equals(getServiceHub().getNetworkMapCache()
                        .getPeerByLegalName(new CordaX500Name("BankOperator", "Toronto", "CA")))) {
                    throw new FlowException("Only Bank Node can send a payment state");
                }
            }
        });

        subFlow(new ReceiveFinalityFlow(counterPartySession, stx.getId()));
        return null;
    }
}