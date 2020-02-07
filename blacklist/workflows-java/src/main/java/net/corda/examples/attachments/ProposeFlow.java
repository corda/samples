package net.corda.examples.attachments;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.attachments.contracts.AgreementContract;
import net.corda.examples.attachments.states.AgreementState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import static net.corda.examples.attachments.contracts.AgreementContract.AGREEMENT_CONTRACT_ID;

@InitiatingFlow
@StartableByRPC
public class ProposeFlow extends FlowLogic<SignedTransaction> {
    private static String agreementTxt;
    private static SecureHash untrustedPartiesAttachment;
    private static Party counterparty;

    public ProposeFlow(String agreementTxt, SecureHash untrustedPartiesAttachment, Party counterparty) {
        this.agreementTxt = agreementTxt;
        this.untrustedPartiesAttachment = untrustedPartiesAttachment;
        this.counterparty = counterparty;
    }

    Party getFirstNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) throw new FlowException("No available notary");
        return notaries.get(0);
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party notary = getFirstNotary();

        final AgreementState agreementState = new AgreementState(getOurIdentity(), counterparty, agreementTxt);
        final AgreementContract.Commands.Agree agreeCmd = new AgreementContract.Commands.Agree();
        final List<PublicKey> agreeCmdRequiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), counterparty.getOwningKey());

        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(agreementState, AGREEMENT_CONTRACT_ID)
                .addCommand(agreeCmd, agreeCmdRequiredSigners)
                .addAttachment(untrustedPartiesAttachment);

        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        final FlowSession counterpartySession = initiateFlow(counterparty);
        final SignedTransaction signedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(counterpartySession)));

        return subFlow(new FinalityFlow(signedTx, counterpartySession));
    }
}
