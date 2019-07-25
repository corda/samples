package negotiation.flows

import co.paralleluniverse.fibers.Suspendable
import negotiation.contracts.ProposalAndTradeContract
import negotiation.states.ProposalState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object ProposalFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val isBuyer: Boolean, val amount: Int, val counterparty: Party) : FlowLogic<UniqueIdentifier>() {
        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): UniqueIdentifier {
            // Creating the output.
            val (buyer, seller) = when {
                isBuyer -> ourIdentity to counterparty
                else -> counterparty to ourIdentity
            }
            val output = ProposalState(amount, buyer, seller, ourIdentity, counterparty)

            // Creating the command.
            val commandType = ProposalAndTradeContract.Commands.Propose()
            val requiredSigners = listOf(ourIdentity.owningKey, counterparty.owningKey)
            val command = Command(commandType, requiredSigners)

            // Building the transaction.
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)
            txBuilder.addOutputState(output, ProposalAndTradeContract.ID)
            txBuilder.addCommand(command)

            // Signing the transaction ourselves.
            val partStx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's signature.
            val counterpartySession = initiateFlow(counterparty)
            val fullyStx = subFlow(CollectSignaturesFlow(partStx, listOf(counterpartySession)))

            // Finalising the transaction.
            val finalisedTx = subFlow(FinalityFlow(fullyStx, listOf(counterpartySession)))
            return finalisedTx.tx.outputsOfType<ProposalState>().single().linearId
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    // No checking to be done.
                }
            }

            val txId = subFlow(signTransactionFlow).id

            subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }
}
