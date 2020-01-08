package corda.tokenSDK.samples.contracts.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
import corda.tokenSDK.samples.states.HouseState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;

import java.util.Currency;
import java.util.UUID;

/**
 * Flow class to update the evolvable Token. TokenSDK provides the UpdateEvolvableToken flow which could be called with the input and outputs. It takes care of the
 * building the transaction and performing the updates as well as notifying the maintainers.
 */
@StartableByRPC
public class UpdateHouseValuationFlow extends FlowLogic<SignedTransaction> {

    private final String houseId;
    private final Amount<Currency> newValuation;

    public UpdateHouseValuationFlow(String houseId, Amount<Currency> newValuation) {
        this.houseId = houseId;
        this.newValuation = newValuation;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        UUID uuid = UUID.fromString(houseId);
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null, ImmutableList.of(uuid), null, Vault.StateStatus.UNCONSUMED);
        StateAndRef<HouseState> input = getServiceHub().getVaultService().
                queryBy(HouseState.class, queryCriteria).getStates().get(0);
        HouseState houseState = input.getState().getData();
        HouseState outputState = new HouseState(houseState.getLinearId(), houseState.getMaintainers(), newValuation,
                houseState.getNoOfBedRooms(), houseState.getConstructionArea(), houseState.getAdditionInfo(), houseState.getAddress());
        return subFlow(new UpdateEvolvableToken(input, outputState));
    }
}
