package net.corda.examples.bikemarket.flows;

import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.examples.bikemarket.states.FrameTokenState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class CreateFrameToken extends FlowLogic<String> {

    final private String frameModel;

    public CreateFrameToken(String frameSerial) {
        this.frameModel = frameSerial;
    }

    @Override
    public String call() throws FlowException {

        //grab the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //Create non-fungible frame token
        UniqueIdentifier uuid = new UniqueIdentifier();
        FrameTokenState frame = new FrameTokenState(getOurIdentity(), uuid, 0 , this.frameModel);

        //warp it with transaction state specifying the notary
        TransactionState transactionState = new TransactionState(frame, notary);

        //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
        subFlow(new CreateEvolvableTokens(transactionState));
        return "\nCreated a frame token for bike frame. (Serial #"+ this.frameModel + ").";
    }
}
