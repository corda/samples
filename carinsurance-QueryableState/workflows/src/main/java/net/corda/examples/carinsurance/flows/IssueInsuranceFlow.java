package net.corda.examples.carinsurance.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.carinsurance.contracts.InsuranceContract;
import net.corda.examples.carinsurance.states.InsuranceState;
import net.corda.examples.carinsurance.states.VehicleDetail;

public class IssueInsuranceFlow {

    private IssueInsuranceFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class IssueInsuranceInitiator extends FlowLogic<SignedTransaction> {

        private final ProgressTracker progressTracker = new ProgressTracker();

        private final InsuranceInfo insuranceInfo;
        private final Party insuree;

        public IssueInsuranceInitiator(InsuranceInfo insuranceInfo, Party insuree) {
            this.insuranceInfo = insuranceInfo;
            this.insuree = insuree;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party insurer = getOurIdentity();

            VehicleInfo vehicleInfo = insuranceInfo.getVehicleInfo();
            VehicleDetail vehicleDetail = new VehicleDetail(vehicleInfo.getRegistrationNumber(),
                    vehicleInfo.getChasisNumber(), vehicleInfo.getMake(), vehicleInfo.getModel(),
                    vehicleInfo.getVariant(), vehicleInfo.getColor(), vehicleInfo.getFuelType());

            // Build the insurance output state.
            InsuranceState insurance = new InsuranceState(insuranceInfo.getPolicyNumber(), insuranceInfo.getInsuredValue(),
                    insuranceInfo.getDuration(), insuranceInfo.getPremium(), insurer, insuree, vehicleDetail,
                    null);

            // Build the transaction
            TransactionBuilder builder = new TransactionBuilder(notary)
                .addOutputState(insurance, InsuranceContract.ID)
                .addCommand(new InsuranceContract.Commands.IssueInsurance(), ImmutableList.of(insurer.getOwningKey()));

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow
            FlowSession ownerSession = initiateFlow(insuree);
            return subFlow(new FinalityFlow(selfSignedTransaction, ImmutableList.of(ownerSession)));
        }
    }

    @InitiatedBy(IssueInsuranceInitiator.class)
    public static class IssueInsuranceResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public IssueInsuranceResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
