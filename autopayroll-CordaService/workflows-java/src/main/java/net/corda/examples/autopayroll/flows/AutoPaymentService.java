package net.corda.examples.autopayroll.flows;

import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.examples.autopayroll.states.PaymentRequestState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.corda.core.identity.Party;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@CordaService
public class AutoPaymentService extends SingletonSerializeAsToken {
    private final static Logger log = LoggerFactory.getLogger(AutoPaymentService.class);
    private final static Executor executor = Executors.newFixedThreadPool(8);
    private final AppServiceHub serviceHub;

    public AutoPaymentService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        directPayment();
        log.info("Tracking new Payment Request");
    }

    private void directPayment() {
        Party ourIdentity = ourIdentity();
        serviceHub.getVaultService().trackBy(PaymentRequestState.class).getUpdates().subscribe(
                update -> {
                    update.getProduced().forEach(
                            message -> {
                                TransactionState<PaymentRequestState> state = message.getState();
                                if (ourIdentity.equals(
                                        serviceHub.getNetworkMapCache().getPeerByLegalName(new CordaX500Name("BankOperator", "Toronto", "CA"))
                                )) {
                                    executor.execute(() -> {
                                        log.info("Directing to message " + state);
                                        serviceHub.startFlow(new PaymentFlow.PaymentFlowInitiator()); // START FLOW HERE
                                    });
                                }
                            }
                    );
                }
        );

    }

    private Party ourIdentity() {
        return serviceHub.getMyInfo().getLegalIdentities().get(0);
    }
}
