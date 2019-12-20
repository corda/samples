package net.corda.examples.stockpaydividend;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.stockpaydividend.flows.*;
import net.corda.examples.stockpaydividend.states.DividendState;
import net.corda.examples.stockpaydividend.states.StockState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FlowTests {
    protected MockNetwork network;
    protected StartedMockNode company;
    protected StartedMockNode observer;
    protected StartedMockNode shareholder;
    protected StartedMockNode bank;
    protected Date exDate;
    protected Date payDate;

    protected StartedMockNode notary;
    protected Party notaryParty;

    public static TestIdentity COMPANY = new TestIdentity(new CordaX500Name("Company", "TestVillage", "US"));
    public static TestIdentity SHAREHOLDER = new TestIdentity(new CordaX500Name("Shareholder", "TestVillage", "US"));
    public static TestIdentity BANK = new TestIdentity(new CordaX500Name("Bank", "Rulerland", "US"));
    public static TestIdentity OBSERVER = new TestIdentity(new CordaX500Name("Observer", "Rulerland", "US"));

    public final static String STOCK_SYMBOL = "TEST";
    public final static String STOCK_NAME = "Test Stock";
    public final static String STOCK_CURRENCY = "USD";
    public final static BigDecimal STOCK_PRICE = BigDecimal.valueOf(7.4);
    public final static Integer ISSUING_STOCK_QUANTITY = 2000;
    public final static Long BUYING_STOCK = Long.valueOf(500);
    public final static Integer ISSUING_MONEY = 5000000;
    public final static BigDecimal ANNOUNCING_DIVIDEND = new BigDecimal("0.03");

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters()
                .withNetworkParameters(new NetworkParameters(
                        4,
                        emptyList(),
                        1000000000,
                        1000000000,
                        Instant.now(),
                        1,
                        emptyMap()))
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.examples.stockpaydividend.contracts"),
                        TestCordapp.findCordapp("net.corda.examples.stockpaydividend.flows"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
                ))
                .withThreadPerNode(false)
        );

        company = network.createPartyNode(COMPANY.getName());
        observer = network.createPartyNode(OBSERVER.getName());
        shareholder = network.createPartyNode(SHAREHOLDER.getName());
        bank = network.createPartyNode(BANK.getName());
        notary = network.getNotaryNodes().get(0);
        notaryParty = notary.getInfo().getLegalIdentities().get(0);

        // Set execution date as tomorrow
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        exDate = c.getTime();

        // Set pay date as the day after tomorrow
        c.add(Calendar.DATE, 1);
        payDate = c.getTime();

        network.startNodes();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void issueTest() throws ExecutionException, InterruptedException {
        // Issue Stock
        CordaFuture<SignedTransaction> future = company.startFlow(new IssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty));
        network.runNetwork();
        SignedTransaction stx = future.get();

        //Check if company and observer of the stock have recorded the transactions
        SignedTransaction issuerTx = company.getServices().getValidatedTransactions().getTransaction(stx.getId());
        SignedTransaction observerTx = observer.getServices().getValidatedTransactions().getTransaction(stx.getId());
        assertNotNull(issuerTx);
        assertNotNull(observerTx);
        assertEquals(issuerTx, observerTx);
    }


    @Test
    public void moveTest() throws ExecutionException, InterruptedException {
        // Issue Stock
        CordaFuture<SignedTransaction> future = company.startFlow(new IssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty));
        network.runNetwork();
        future.get();

        // Move Stock
        future = company.startFlow(new MoveStock.Initiator(STOCK_SYMBOL, BUYING_STOCK, shareholder.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        SignedTransaction moveTx = future.get();

        //Retrieve states from receiver
        List<StateAndRef<StockState>> receivedStockStatesPages = shareholder.getServices().getVaultService().queryBy(StockState.class).getStates();
        StockState receivedStockState = receivedStockStatesPages.get(0).getState().getData();
        Amount<TokenPointer> receivedAmount = QueryUtilitiesKt.tokenBalance(shareholder.getServices().getVaultService(), receivedStockState.toPointer(receivedStockState.getClass()));

        //Check
        assertEquals(receivedAmount.getQuantity(), Long.valueOf(500).longValue());

        //Retrieve states from sender
        List<StateAndRef<StockState>> remainingStockStatesPages = company.getServices().getVaultService().queryBy(StockState.class).getStates();
        StockState remainingStockState = remainingStockStatesPages.get(0).getState().getData();
        Amount<TokenPointer> remainingAmount = QueryUtilitiesKt.tokenBalance(company.getServices().getVaultService(), remainingStockState.toPointer(remainingStockState.getClass()));

        //Check
        assertEquals(remainingAmount.getQuantity(), Long.valueOf(1500).longValue());

    }

    @Test
    public void announceDividendTest() throws ExecutionException, InterruptedException {
        // Issue Stock
        CordaFuture<SignedTransaction> future = company.startFlow(new IssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty));
        network.runNetwork();
        future.get();

        // Move Stock
        future = company.startFlow(new MoveStock.Initiator(STOCK_SYMBOL, BUYING_STOCK, shareholder.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        future.get();

        // Announce Dividend
        future = company.startFlow(new AnnounceDividend.Initiator(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate, payDate));
        network.runNetwork();
        SignedTransaction announceTx = future.get();

        // Retrieve states from sender
        List<StateAndRef<StockState>> remainingStockStatesPages = company.getServices().getVaultService().queryBy(StockState.class).getStates();
        StockState remainingStockState = remainingStockStatesPages.get(0).getState().getData();
        assertEquals(remainingStockState.getDividend(), ANNOUNCING_DIVIDEND);
        assertEquals(remainingStockState.getExDate(), exDate);
        assertEquals(remainingStockState.getPayDate(), payDate);

        // Check observer has recorded the same transaction
        SignedTransaction issuerTx = company.getServices().getValidatedTransactions().getTransaction(announceTx.getId());
        SignedTransaction observerTx = observer.getServices().getValidatedTransactions().getTransaction(announceTx.getId());

        assertNotNull(issuerTx);
        assertNotNull(observerTx);
        assertEquals(issuerTx, observerTx);

    }

    @Test
    public void getStockUpdateTest() throws ExecutionException, InterruptedException {
        // Issue Stock
        CordaFuture<SignedTransaction> future = company.startFlow(new IssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty));
        network.runNetwork();
        future.get();

        // Move Stock
        future = company.startFlow(new MoveStock.Initiator(STOCK_SYMBOL, BUYING_STOCK, shareholder.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        future.get();

        // Announce Dividend
        future = company.startFlow(new AnnounceDividend.Initiator(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate, payDate));
        network.runNetwork();
        future.get();

        // Get Stock Update
        future = shareholder.startFlow(new GetStockUpdate.Initiator(STOCK_SYMBOL));
        network.runNetwork();
        future.get();

        // Checks if the shareholder actually receives the same transaction and updated the stock state (with new dividend)
        List<StateAndRef<StockState>> issuerStockStateRefs = company.getServices().getVaultService().queryBy(StockState.class).getStates();
        StateRef issuerStateRef = issuerStockStateRefs.get(0).getRef();
        List<StateAndRef<StockState>> holderStockStateRefs = shareholder.getServices().getVaultService().queryBy(StockState.class).getStates();
        StateRef holderStateRef = holderStockStateRefs.get(0).getRef();

        assertEquals(issuerStateRef.getTxhash(), holderStateRef.getTxhash());
    }


    @Test
    public void claimDividendTest() throws ExecutionException, InterruptedException {
        // Issue Stock
        CordaFuture<SignedTransaction> future = company.startFlow(new IssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty));
        network.runNetwork();
        future.get();

        // Move Stock
        future = company.startFlow(new MoveStock.Initiator(STOCK_SYMBOL, BUYING_STOCK, shareholder.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        SignedTransaction moveTx = future.get();

        // Announce Dividend
        future = company.startFlow(new AnnounceDividend.Initiator(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate, payDate));
        network.runNetwork();
        future.get();

        // Shareholder claims Dividend
        future = shareholder.startFlow(new GetStockUpdate.Initiator(STOCK_SYMBOL));
        network.runNetwork();
        future.get();

        // Shareholder claims Dividend
        future = shareholder.startFlow(new ClaimDividendReceivable.Initiator(STOCK_SYMBOL));
        network.runNetwork();
        SignedTransaction claimTx = future.get();

        // Checks if the dividend amount is correct
        List<StateAndRef<DividendState>> holderDividendPages = shareholder.getServices().getVaultService().queryBy(DividendState.class).getStates();
        DividendState holderState = holderDividendPages.get(0).getState().getData();

        Integer fractionalDigit = holderState.getDividendAmount().getToken().getFractionDigits();
        BigDecimal yield = BigDecimal.valueOf(BUYING_STOCK).multiply(ANNOUNCING_DIVIDEND);
        BigDecimal receivingDividend = yield.multiply(STOCK_PRICE).multiply(BigDecimal.valueOf(Math.pow(10.0, fractionalDigit)));

        assertEquals(holderState.getDividendAmount().getQuantity(), receivingDividend.longValue());

        // Check company and shareholder owns the same transaction
        SignedTransaction issuerTx = company.getServices().getValidatedTransactions().getTransaction(claimTx.getId());
        SignedTransaction holderTx = shareholder.getServices().getValidatedTransactions().getTransaction(claimTx.getId());

        assertNotNull(issuerTx);
        assertNotNull(holderTx);
        assertEquals(issuerTx, holderTx);

    }

    @Test
    public void payDividendTest() throws ExecutionException, InterruptedException {
        // Issue Money
        CordaFuture<SignedTransaction> future = bank.startFlow(new IssueMoney(STOCK_CURRENCY, ISSUING_MONEY.longValue(), company.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        future.get();

        // Issue Stock
        future = company.startFlow(new IssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty));
        network.runNetwork();
        future.get();

        // Move Stock
        future = company.startFlow(new MoveStock.Initiator(STOCK_SYMBOL, BUYING_STOCK, shareholder.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        future.get();

        // Announce Dividend
        future = company.startFlow(new AnnounceDividend.Initiator(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate, payDate));
        network.runNetwork();
        future.get();

        // Shareholder claims Dividend
        future = shareholder.startFlow(new GetStockUpdate.Initiator(STOCK_SYMBOL));
        network.runNetwork();
        future.get();

        // Shareholder claims Dividend
        future = shareholder.startFlow(new ClaimDividendReceivable.Initiator(STOCK_SYMBOL));
        network.runNetwork();
        future.get();

        //Pay Dividend
        CordaFuture<List<SignedTransaction>> futurePayDiv = company.startFlow(new PayDividend.Initiator());
        network.runNetwork();
        List<SignedTransaction> txList = futurePayDiv.get();

        // The above test should only have 1 transaction created
        assertEquals(txList.size(),  1);
        SignedTransaction payDivTx = txList.get(0);

        // Checks if no Dividend state left unspent in shareholder's and company's vault
        List<StateAndRef<DividendState>> holderDivStateRefs = shareholder.getServices().getVaultService().queryBy(DividendState.class).getStates();
        assert(holderDivStateRefs.isEmpty());
        List<StateAndRef<DividendState>> issuerDivStateRefs = company.getServices().getVaultService().queryBy(DividendState.class).getStates();
        assert(issuerDivStateRefs.isEmpty());

        // Validates shareholder has received equivalent fiat currencies of the dividend
        TokenType fiatTokenType = FiatCurrency.Companion.getInstance(STOCK_CURRENCY);
        Amount<TokenType> fiatAmount = QueryUtilitiesKt.tokenBalance(shareholder.getServices().getVaultService(), fiatTokenType);
        BigDecimal receivingDividend = BigDecimal.valueOf(BUYING_STOCK).multiply(ANNOUNCING_DIVIDEND);
        assertEquals(fiatAmount.getQuantity(), receivingDividend.longValue());

        // Check company and shareholder owns the same transaction
        SignedTransaction issuerTx = company.getServices().getValidatedTransactions().getTransaction(payDivTx.getId());
        SignedTransaction holderTx = shareholder.getServices().getValidatedTransactions().getTransaction(payDivTx.getId());
        assertNotNull(issuerTx);
        assertNotNull(holderTx);
        assertEquals(issuerTx, holderTx);
}


}
