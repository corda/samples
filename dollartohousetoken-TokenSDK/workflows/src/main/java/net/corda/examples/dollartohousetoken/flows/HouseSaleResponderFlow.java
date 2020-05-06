package net.corda.examples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.TokenQueryBy;
import com.r3.corda.lib.tokens.selection.TokenQueryByKt;
import com.r3.corda.lib.tokens.selection.database.config.DatabaseSelectionConfigKt;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

/*
* Responder Flow for the sale of house in exchage of fiat-currency. This flow receives the valuation of the house from the seller and transfer the equivalent
* amount of fiat currency to the seller.
* */
@InitiatedBy(HouseSaleInitiatorFlow.class)
public class HouseSaleResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession counterpartySession;

    public HouseSaleResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        /* Recieve the valuation of the house */
        Amount<Currency> price =  counterpartySession.receive(Amount.class).unwrap(amount -> amount);

        /* Create instance of the fiat currecy token amount */
        Amount<TokenType> priceToken = new Amount<>(price.getQuantity(), FiatCurrency.Companion.getInstance(price.getToken().getCurrencyCode()));

        /* Create an instance of the TokenSelection object, it is used to select the token from the vault and generate the proposal for the movement of the token
        *  The constructor takes the service hub to perform vault query, the max-number of retries, the retry sleep interval, and the retry sleep cap interval. This
        *  is a temporary solution till in-memory token selection in implemented.
        * */
        DatabaseTokenSelection tokenSelection = new DatabaseTokenSelection(
                getServiceHub(),
                DatabaseSelectionConfigKt.MAX_RETRIES_DEFAULT,
                DatabaseSelectionConfigKt.RETRY_SLEEP_DEFAULT,
                DatabaseSelectionConfigKt.RETRY_CAP_DEFAULT,
                DatabaseSelectionConfigKt.PAGE_SIZE_DEFAULT
        );

        /*
        *  Generate the move proposal, it returns the input-output pair for the fiat currency transfer, which we need to send to the Initiator.
        * */
        Pair<AbstractParty, Amount<TokenType>> partyAndAmount = new Pair<>(counterpartySession.getCounterparty(), priceToken);
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs =
                tokenSelection.generateMove(Arrays.asList(partyAndAmount), getOurIdentity(), new TokenQueryBy(), getRunId().getUuid());

        /* Call SendStateAndRefFlow to send the inputs to the Initiator*/
        subFlow(new SendStateAndRefFlow(counterpartySession, inputsAndOutputs.getFirst()));
        /* Send the output generated from the fiat currency move proposal to the initiator */
        counterpartySession.send(inputsAndOutputs.getSecond());

        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
