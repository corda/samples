package net.corda.examples.obligation.flows;

import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.obligation.Obligation;
import org.junit.Test;

import static net.corda.finance.Currencies.POUNDS;
import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;
import static org.junit.Assert.assertEquals;

public class IssueObligationTests extends ObligationTests {

    @Test
    public void issueNonAnonymousObligationSuccessfully() throws Exception {
        SignedTransaction stx = issueObligation(a, b, POUNDS(1000), false);

        network.waitQuiescent();

        Obligation aObligation = (Obligation) a.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();
        Obligation bObligation = (Obligation) b.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();

        assertEquals(aObligation, bObligation);
    }

    @Test
    public void issueAnonymousObligationSuccessfully() throws Exception {
        SignedTransaction stx = issueObligation(a, b, POUNDS(1000), true);

        Party aIdentity = chooseIdentity(a.getServices().getMyInfo());
        Party bIdentity = chooseIdentity(b.getServices().getMyInfo());

        network.waitQuiescent();

        Obligation aObligation = (Obligation) a.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();
        Obligation bObligation = (Obligation) b.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();

        assertEquals(aObligation, bObligation);

        Party maybePartyALookedUpByA = a.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aObligation.getBorrower());
        Party maybePartyALookedUpByB = b.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aObligation.getBorrower());

        assertEquals(aIdentity, maybePartyALookedUpByA);
        assertEquals(aIdentity, maybePartyALookedUpByB);

        Party maybePartyCLookedUpByA = a.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aObligation.getLender());
        Party maybePartyCLookedUpByB = b.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aObligation.getLender());

        assertEquals(bIdentity, maybePartyCLookedUpByA);
        assertEquals(bIdentity, maybePartyCLookedUpByB);
    }
}
