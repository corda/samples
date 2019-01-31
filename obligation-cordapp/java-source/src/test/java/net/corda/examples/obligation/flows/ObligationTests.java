package net.corda.examples.obligation.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;

import java.util.Currency;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;

/**
 * A base class to reduce the boilerplate when writing obligation flow tests.
 */
abstract class ObligationTests {
    protected MockNetwork network;
    protected StartedMockNode a;
    protected StartedMockNode b;
    protected StartedMockNode c;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters()
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.finance.schemas"),
                        TestCordapp.findCordapp("net.corda.examples.obligation")))
                .withThreadPerNode(true));

        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);

        for (StartedMockNode node : ImmutableList.of(a, b, c)) {
            node.registerInitiatedFlow(IssueObligation.Responder.class);
            node.registerInitiatedFlow(TransferObligation.Responder.class);
        }
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    protected SignedTransaction issueObligation(StartedMockNode borrower,
                                                StartedMockNode lender,
                                                Amount<Currency> amount,
                                                Boolean anonymous) throws InterruptedException, ExecutionException {
        Party lenderIdentity = chooseIdentity(lender.getInfo());
        IssueObligation.Initiator flow = new IssueObligation.Initiator(amount, lenderIdentity, anonymous);
        return borrower.startFlow(flow).get();
    }

    protected SignedTransaction transferObligation(UniqueIdentifier linearId,
                                                   StartedMockNode lender,
                                                   StartedMockNode newLender,
                                                   Boolean anonymous) throws InterruptedException, ExecutionException {
        Party newLenderIdentity = chooseIdentity(newLender.getInfo());
        TransferObligation.Initiator flow = new TransferObligation.Initiator(linearId, newLenderIdentity, anonymous);
        return lender.startFlow(flow).get();
    }

    protected SignedTransaction settleObligation(UniqueIdentifier linearId,
                                                 StartedMockNode borrower,
                                                 Amount<Currency> amount,
                                                 Boolean anonymous) throws InterruptedException, ExecutionException {
        SettleObligation.Initiator flow = new SettleObligation.Initiator(linearId, amount, anonymous);
        return borrower.startFlow(flow).get();
    }

    protected SignedTransaction selfIssueCash(StartedMockNode party,
                                              Amount<Currency> amount) throws InterruptedException, ExecutionException {
        Party notary = party.getServices().getNetworkMapCache().getNotaryIdentities().get(0);
        OpaqueBytes issueRef = OpaqueBytes.of("0".getBytes());
        CashIssueFlow.IssueRequest issueRequest = new CashIssueFlow.IssueRequest(amount, issueRef, notary);
        CashIssueFlow flow = new CashIssueFlow(issueRequest);
        return party.startFlow(flow).get().getStx();
    }
}
