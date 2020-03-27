package net.corda.examples.bikemarket.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.examples.bikemarket.states.FrameTokenState;
import net.corda.examples.bikemarket.states.WheelsTokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;

@InitiatingFlow
@StartableByRPC
public class IssueNewBike extends FlowLogic<String> {

    private final String frameModel;
    private final String wheelsModel;
    private final Party holder;

    public IssueNewBike(String frameSerial, String wheelSerial, Party holder) {
        this.frameModel = frameSerial;
        this.wheelsModel = wheelSerial;
        this.holder = holder;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {

        //Step 1: Frame Token
        //get frame states on ledger
        StateAndRef<FrameTokenState> frameStateAndRef = getServiceHub().getVaultService().
                queryBy(FrameTokenState.class).getStates().stream()
                .filter(sf->sf.getState().getData().getModelNum().equals(this.frameModel)).findAny()
                .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+this.frameModel+"\" not found from vault"));

        //get the TokenType object
        FrameTokenState frametokentype = frameStateAndRef.getState().getData();

        //get the pointer pointer to the frame
        TokenPointer frametokenPointer = frametokentype.toPointer(frametokentype.getClass());

        //assign the issuer to the frame type who will be issuing the tokens
        IssuedTokenType frameissuedTokenType = new IssuedTokenType(getOurIdentity(), frametokenPointer);

        //mention the current holder also
        NonFungibleToken frametoken = new NonFungibleToken(frameissuedTokenType, holder, new UniqueIdentifier(), TransactionUtilitiesKt.getAttachmentIdForGenericParam(frametokenPointer));


        //Step 2: Wheels Token
        StateAndRef<WheelsTokenState> wheelStateStateAndRef = getServiceHub().getVaultService().
                queryBy(WheelsTokenState.class).getStates().stream().filter(sf->sf.getState().getData().getModelNum().equals(this.wheelsModel)).findAny()
                .orElseThrow(()-> new IllegalArgumentException("StockState symbol=\""+this.wheelsModel+"\" not found from vault"));

        //get the TokenType object
        WheelsTokenState wheeltokentype = wheelStateStateAndRef.getState().getData();

        //get the pointer pointer to the wheel
        TokenPointer wheeltokenPointer = wheeltokentype.toPointer(wheeltokentype.getClass());

        //assign the issuer to the wheel type who will be issuing the tokens
        IssuedTokenType wheelissuedTokenType = new IssuedTokenType(getOurIdentity(), wheeltokenPointer);

        //mention the current holder also
        NonFungibleToken wheeltoken = new NonFungibleToken(wheelissuedTokenType, holder, new UniqueIdentifier(), TransactionUtilitiesKt.getAttachmentIdForGenericParam(wheeltokenPointer));

        //distribute the new bike (two token to be exact)
        //call built in flow to issue non fungible tokens
        SignedTransaction stx = subFlow(new IssueTokens(Arrays.asList(frametoken,wheeltoken)));

        return "\nA new bike is being issued to "+ this.holder.getName().getOrganisation() + " with frame model: "
                + this.frameModel + "; wheels model: "+ this.wheelsModel + "\nTransaction ID: " + stx.getId();
    }
}
