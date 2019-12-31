package net.corda.examples.carinsurance.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.carinsurance.contracts.InsuranceContract;
import net.corda.examples.carinsurance.states.Claim;
import net.corda.examples.carinsurance.states.InsuranceState;

import java.util.ArrayList;
import java.util.List;

public class InsuranceClaimFlow {

    private InsuranceClaimFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceClaimInitiator extends FlowLogic<SignedTransaction>{

        private final ClaimInfo claimInfo;
        private final String policyNumber;

        public InsuranceClaimInitiator(ClaimInfo claimInfo, String policyNumber) {
            this.claimInfo = claimInfo;
            this.policyNumber = policyNumber;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all Insurance state, and filter the results based on the policyNumber
            // to fetch the desired Insurance state from the vault. This filtered state would be used as input to the
            // transaction.
            List<StateAndRef<InsuranceState>> insuranceStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(InsuranceState.class).getStates();

            StateAndRef<InsuranceState> inputStateAndRef = insuranceStateAndRefs.stream().filter(insuranceStateAndRef -> {
                InsuranceState insuranceState = insuranceStateAndRef.getState().getData();
                return insuranceState.getPolicyNumber().equals(policyNumber);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Policy Not Found"));

            Claim claim = new Claim(claimInfo.getClaimNumber(), claimInfo.getClaimDescription(),
                    claimInfo.getClaimAmount());
            InsuranceState input = inputStateAndRef.getState().getData();

            List<Claim> claims = new ArrayList<>();
            if(input.getClaims() == null || input.getClaims().size() == 0 ){
                claims.add(claim);
            }else {
                claims.addAll(input.getClaims());
                claims.add(claim);
            }

            //Create the output state
            InsuranceState output = new InsuranceState(input.getPolicyNumber(), input.getInsuredValue(),
                    input.getDuration(), input.getPremium(), input.getInsurer(), input.getInsuree(),
                    input.getVehicleDetail(), claims);

            // Build the transaction.
            TransactionBuilder transactionBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, InsuranceContract.ID)
                    .addCommand(new InsuranceContract.Commands.AddClaim(), ImmutableList.of(getOurIdentity().getOwningKey()));

            // Verify the transaction
            transactionBuilder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // Call finality Flow
            FlowSession counterpartySession = initiateFlow(input.getInsuree());
            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(counterpartySession)));
        }
    }

    @InitiatedBy(InsuranceClaimInitiator.class)
    public static class InsuranceClaimResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public InsuranceClaimResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
