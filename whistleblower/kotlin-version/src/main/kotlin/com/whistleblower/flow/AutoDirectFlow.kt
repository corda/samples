package com.whistleblower.flow

import co.paralleluniverse.fibers.Suspendable
import com.whistleblower.contract.AUTODIRECT_ID
import com.whistleblower.contract.AutoDirectContract
import com.whistleblower.state.AutoDirectState
import com.whistleblower.state.BlowWhistleState
import net.corda.core.contracts.Command
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * Blows the whistle on a company.
 *
 * Confidential identities are used to preserve the identity of the whistle-blower and the investigator.
 *
 * @param badCompany the company the whistle is being blown on.
 * @param investigator the party handling the investigation.
 */
@InitiatingFlow
@StartableByService
class AutoDirectFlow() : FlowLogic<SignedTransaction>() {

    //private val investigator = Party(KeyStore.getX509Certificate("blahblahblah"))

    companion object {
        object GENERATE_CONFIDENTIAL_IDS : Step("Generating confidential identities for the transaction.") {
            override fun childProgressTracker() = SwapIdentitiesFlow.tracker()
        }

        object BUILD_TRANSACTION : Step("Building the transaction.")
        object VERIFY_TRANSACTION : Step("Verifying the transaction.")
        object SIGN_TRANSACTION : Step("I sign the transaction.")
        object COLLECT_COUNTERPARTY_SIG : Step("The counterparty signs the transaction.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISE_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATE_CONFIDENTIAL_IDS,
                BUILD_TRANSACTION,
                VERIFY_TRANSACTION,
                SIGN_TRANSACTION,
                COLLECT_COUNTERPARTY_SIG,
                FINALISE_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val investigator = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("TradeBody", "Kisumu", "KE"))!!
        progressTracker.currentStep = GENERATE_CONFIDENTIAL_IDS
        val investigatorSession = initiateFlow(investigator)
        val (anonymousMe, anonymousInvestigator)
                = generateConfidentialIdentities(investigatorSession)



        progressTracker.currentStep = BUILD_TRANSACTION


        val wBStateList =  serviceHub.vaultService.queryBy(BlowWhistleState::class.java).states
        val vaultState = wBStateList.get(wBStateList.size -1).state.data
        val output = AutoDirectState(vaultState.badCompany,anonymousInvestigator)






        val command = Command(AutoDirectContract.Commands.AutoDirect(), listOf(anonymousMe.owningKey, anonymousInvestigator.owningKey))
        val txBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(output, AUTODIRECT_ID)
                .addCommand(command)

        progressTracker.currentStep = VERIFY_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGN_TRANSACTION
        val stx = serviceHub.signInitialTransaction(txBuilder, anonymousMe.owningKey)

        progressTracker.currentStep = COLLECT_COUNTERPARTY_SIG
        val ftx = subFlow(CollectSignaturesFlow(
                stx,
                listOf(investigatorSession),
                listOf(anonymousMe.owningKey),
                Companion.COLLECT_COUNTERPARTY_SIG.childProgressTracker()))

        progressTracker.currentStep = FINALISE_TRANSACTION
        return subFlow(FinalityFlow(ftx, listOf(investigatorSession), Companion.FINALISE_TRANSACTION.childProgressTracker()))
    }

    /** Generates confidential identities for the whistle-blower and the investigator. */
    @Suspendable
    private fun generateConfidentialIdentities(counterpartySession: FlowSession): Pair<AnonymousParty, AnonymousParty> {
        val confidentialIdentities = subFlow(SwapIdentitiesFlow(
                counterpartySession,
                Companion.GENERATE_CONFIDENTIAL_IDS.childProgressTracker()))
        val anonymousMe = confidentialIdentities[ourIdentity]!!
        val anonymousInvestigator = confidentialIdentities[counterpartySession.counterparty]!!
        return anonymousMe to anonymousInvestigator
    }
}

@InitiatedBy(AutoDirectFlow::class)
class AutoDirectFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(SwapIdentitiesFlow(counterpartySession))

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: Checking.
            }
        }

        val txId = subFlow(signTransactionFlow).id

        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}
