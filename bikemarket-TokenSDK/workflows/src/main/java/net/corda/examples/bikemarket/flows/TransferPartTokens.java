package net.corda.examples.bikemarket.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken;
import net.corda.examples.bikemarket.states.FrameTokenState;
import net.corda.examples.bikemarket.states.WheelsTokenState;
import kotlin.Unit;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

public class TransferPartTokens {

    public  TransferPartTokens() {
    }

    @InitiatingFlow
    @StartableByRPC
    public static class TransferPartToken extends FlowLogic<String> {

        private String frameModel;
        private String wheelModel;
        private boolean frame = false;
        private final Party holder;


        public TransferPartToken(String part, String serialNumber, Party holder) {
            this.holder = holder;
            if (part.equals("frame")) {
                this.frame = true;
                this.frameModel = serialNumber;
            } else {
                this.wheelModel = serialNumber;
            }
        }

        @Suspendable
        @Override
        public String call() throws FlowException {
            if (frame) {
                StateAndRef<FrameTokenState> frameStateAndRef = getServiceHub().getVaultService().
                        queryBy(FrameTokenState.class).getStates().stream()
                        .filter(sf -> sf.getState().getData().getModelNum().equals(this.frameModel)).findAny()
                        .orElseThrow(() -> new IllegalArgumentException("StockState symbol=\"" + this.frameModel + "\" not found from vault"));

                //get the TokenType object
                FrameTokenState frametokentype = frameStateAndRef.getState().getData();
                Party issuer = frametokentype.getIssuer();

                //get the pointer pointer to the frame
                TokenPointer frametokenPointer = frametokentype.toPointer(frametokentype.getClass());
                PartyAndToken partyAndFrameToken = new PartyAndToken(holder, frametokenPointer);

                SignedTransaction stx = (SignedTransaction) subFlow(new MoveNonFungibleTokens(partyAndFrameToken));
                return "Transfer ownership of the frame ("+this.frameModel+") to" +this.holder.getName().getOrganisation()
                        + "\nTransaction ID: " + stx.getId();

            } else {
                //Step 2: Wheels Token
                StateAndRef<WheelsTokenState> wheelStateStateAndRef = getServiceHub().getVaultService().
                        queryBy(WheelsTokenState.class).getStates().stream().filter(sf -> sf.getState().getData().getModelNum().equals(this.wheelModel)).findAny()
                        .orElseThrow(() -> new IllegalArgumentException("StockState symbol=\"" + this.wheelModel + "\" not found from vault"));

                //get the TokenType object
                WheelsTokenState wheeltokentype = wheelStateStateAndRef.getState().getData();
                Party issuer = wheeltokentype.getIssuer();

                //get the pointer pointer to the wheel
                TokenPointer wheeltokenPointer = wheeltokentype.toPointer(wheeltokentype.getClass());
                PartyAndToken partyAndWheelToken = new PartyAndToken(holder, wheeltokenPointer);

                SignedTransaction stx = (SignedTransaction) subFlow(new MoveNonFungibleTokens(partyAndWheelToken));
                return "Transfer ownership of the wheels (" +this.wheelModel+") to" +this.holder.getName().getOrganisation()
                        + "\nTransaction ID: " + stx.getId();
            }
        }
    }

    @InitiatedBy(TransferPartToken.class)
    public static class TransferPartTokenResponder extends FlowLogic<Unit> {

        private FlowSession counterSession;

        public TransferPartTokenResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveNonFungibleTokensHandler(counterSession));
        }
    }
}
