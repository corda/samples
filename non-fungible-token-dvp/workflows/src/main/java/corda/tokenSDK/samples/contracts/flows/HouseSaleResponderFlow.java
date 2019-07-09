package corda.tokenSDK.samples.contracts.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.List;

@InitiatedBy(HouseSaleInitiatorFlow.class)
public class HouseSaleResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession counterpartySession;

    public HouseSaleResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Amount<Currency> price =  counterpartySession.receive(Amount.class).unwrap(amount -> amount);
        Amount<FiatCurrency> priceToken = new Amount<>(price.getQuantity(), new FiatCurrency(price.getToken()));
        TokenSelection tokenSelection = new TokenSelection(getServiceHub(), 8, 100, 2000);
        PartyAndAmount<FiatCurrency> partyAndAmount = new PartyAndAmount<>(counterpartySession.getCounterparty(), priceToken);
        Pair<List<StateAndRef<FungibleToken<FiatCurrency>>>, List<FungibleToken<FiatCurrency>>> inputsAndOutputs =
                tokenSelection.generateMove(getRunId().getUuid(), ImmutableList.of(partyAndAmount), getOurIdentity(), null);

        subFlow(new SendStateAndRefFlow(counterpartySession, inputsAndOutputs.getFirst()));
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
