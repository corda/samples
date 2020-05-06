package net.corda.examples.bikemarket.flows;

import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.examples.bikemarket.states.WheelsTokenState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class CreateWheelToken extends FlowLogic<String> {

    final private String wheelModel;

    public CreateWheelToken(String wheelSerial) {
        this.wheelModel = wheelSerial;
    }

    @Override
    public String call() throws FlowException {

        //grab the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //Create non-fungible frame token
        UniqueIdentifier uuid = new UniqueIdentifier();
        WheelsTokenState frame = new WheelsTokenState(getOurIdentity(), uuid, 0 , this.wheelModel);

        //warp it with transaction state specifying the notary
        TransactionState transactionState = new TransactionState(frame, notary);

        //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
        subFlow(new CreateEvolvableTokens(transactionState));
        return "\nCreated a wheel token for bike wheels. (Serial #"+ this.wheelModel + ").";
    }
}
