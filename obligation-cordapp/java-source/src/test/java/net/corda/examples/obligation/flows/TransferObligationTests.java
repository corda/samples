package net.corda.examples.obligation.flows;

import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.obligation.Obligation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static net.corda.finance.Currencies.POUNDS;
import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class TransferObligationTests extends ObligationTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void transferNonAnonymousObligationSuccessfully() throws Exception {
        // Issue obligation.
        SignedTransaction issuanceTransaction = issueObligation(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        Obligation issuedObligation = (Obligation) issuanceTransaction.getTx().getOutputStates().get(0);

        // Transfer obligation.
        SignedTransaction transferTransaction = transferObligation(issuedObligation.getLinearId(), b, c, false);
        network.waitQuiescent();
        Obligation transferredObligation = (Obligation) transferTransaction.getTx().getOutputStates().get(0);

        // Check the issued obligation with the new lender is the transferred obligation
        assertEquals(issuedObligation.withNewLender(chooseIdentity(c.getInfo())), transferredObligation);

        // Check everyone has the transfer transaction.
        Obligation aObligation = (Obligation) a.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        Obligation bObligation = (Obligation) b.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        Obligation cObligation = (Obligation) c.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        assertEquals(aObligation, bObligation);
        assertEquals(bObligation, cObligation);
    }

    @Test
    public void transferAnonymousObligationSuccessfully() throws Exception {
        // Issue obligation.
        SignedTransaction issuanceTransaction = issueObligation(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        Obligation issuedObligation = (Obligation) issuanceTransaction.getTx().getOutputStates().get(0);

        // Transfer obligation.
        SignedTransaction transferTransaction = transferObligation(issuedObligation.getLinearId(), b, c, true);
        network.waitQuiescent();
        Obligation transferredObligation = (Obligation) transferTransaction.getTx().getOutputStates().get(0);

        // Check the issued obligation with the new lender is the transferred obligation.
        assertEquals(issuedObligation.withNewLender(transferredObligation.getLender()), transferredObligation);

        // Check everyone has the transfer transaction.
        Obligation aObligation = (Obligation) a.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        Obligation bObligation = (Obligation) b.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        Obligation cObligation = (Obligation) c.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        assertEquals(aObligation, bObligation);
        assertEquals(bObligation, cObligation);
    }

    @Test
    public void transferFlowCanOnlyBeStartedByLender() throws Exception {
        // Issue obligation.
        SignedTransaction issuanceTransaction = issueObligation(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        Obligation issuedObligation = (Obligation) issuanceTransaction.getTx().getOutputStates().get(0);

        // Transfer obligation.
        exception.expectCause(instanceOf(IllegalStateException.class));
        transferObligation(issuedObligation.getLinearId(), a, c, false);
    }
}
