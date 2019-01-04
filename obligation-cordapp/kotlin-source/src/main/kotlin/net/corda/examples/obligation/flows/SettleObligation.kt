package net.corda.examples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.obligation.ObligationContract
import net.corda.examples.obligation.ObligationContract.Companion.OBLIGATION_CONTRACT_ID
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.asset.PartyAndAmount
import net.corda.finance.contracts.getCashBalance
import java.util.*

object SettleObligation {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val linearId: UniqueIdentifier,
                    private val amount: Amount<Currency>,
                    private val anonymous: Boolean = true) : ObligationBaseFlow() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object PREPARATION : ProgressTracker.Step("Obtaining Obligation from vault.")
            object BUILDING : ProgressTracker.Step("Building and verifying transaction.")
            object SIGNING : ProgressTracker.Step("signing transaction.")
            object COLLECTING : ProgressTracker.Step("Collecting counterparty signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(PREPARATION, BUILDING, SIGNING, COLLECTING, FINALISING)
        }

        @Suspendable
        override fun call(): SignedTransaction {
            // Stage 1. Retrieve obligation specified by linearId from the vault.
            progressTracker.currentStep = PREPARATION
            val obligationToSettle = getObligationByLinearId(linearId)
            val inputObligation = obligationToSettle.state.data

            // Stage 2. Resolve the lender and borrower identity if the obligation is anonymous.
            val borrowerIdentity = resolveIdentity(inputObligation.borrower)
            val lenderIdentity = resolveIdentity(inputObligation.lender)

            // Stage 3. This flow can only be initiated by the current recipient.
            check(borrowerIdentity == ourIdentity) {
                throw FlowException("Settle Obligation flow must be initiated by the borrower.")
            }

            // Stage 4. Check we have enough cash to settle the requested amount.
            val cashBalance = serviceHub.getCashBalance(amount.token)
            val amountLeftToSettle = inputObligation.amount - inputObligation.paid
            check(cashBalance.quantity > 0L) {
                throw FlowException("Borrower has no ${amount.token} to settle.")
            }
            check(cashBalance >= amount) {
                throw FlowException("Borrower has only $cashBalance but needs $amount to settle.")
            }
            check(amountLeftToSettle >= amount) {
                throw FlowException("There's only $amountLeftToSettle left to settle but you pledged $amount.")
            }

            // Stage 5. Create a settle command.
            val settleCommand = Command(
                    ObligationContract.Commands.Settle(),
                    inputObligation.participants.map { it.owningKey })

            // Stage 6. Create a transaction builder. Add the settle command and input obligation.
            progressTracker.currentStep = BUILDING
            val builder = TransactionBuilder(firstNotary)
                    .addInputState(obligationToSettle)
                    .addCommand(settleCommand)

            // Stage 7. Get some cash from the vault and add a spend to our transaction builder.
            // We pay cash to the lenders obligation key.
            val lenderPaymentKey = inputObligation.lender
            val (_, cashSigningKeys) = Cash.generateSpend(serviceHub, builder, listOf(PartyAndAmount(lenderPaymentKey, amount)), ourIdentityAndCert)

            // Stage 8. Only add an output obligation state if the obligation has not been fully settled.
            val amountRemaining = amountLeftToSettle - amount
            if (amountRemaining > Amount.zero(amount.token)) {
                val outputObligation = inputObligation.pay(amount)
                builder.addOutputState(outputObligation, OBLIGATION_CONTRACT_ID)
            }

            // Stage 9. Verify and sign the transaction.
            progressTracker.currentStep = SIGNING
            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder, cashSigningKeys + inputObligation.borrower.owningKey)

            // Stage 10. Get counterparty signature.
            progressTracker.currentStep = COLLECTING
            val session = initiateFlow(lenderIdentity)
            subFlow(IdentitySyncFlow.Send(session, ptx.tx))
            val stx = subFlow(CollectSignaturesFlow(
                    ptx,
                    setOf(session),
                    cashSigningKeys + inputObligation.borrower.owningKey,
                    COLLECTING.childProgressTracker())
            )

            // Stage 11. Finalize the transaction.
            progressTracker.currentStep = FINALISING
            return subFlow(FinalityFlow(stx, setOf(session), FINALISING.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val otherFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            subFlow(IdentitySyncFlow.Receive(otherFlow))
            val stx = subFlow(SignTxFlowNoChecking(otherFlow))
            return subFlow(ReceiveFinalityFlow(otherFlow, stx.id))
        }
    }
}
