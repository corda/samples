package net.corda.demos.crowdFunding.contracts

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import net.corda.demos.crowdFunding.keysFromParticipants
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import net.corda.finance.contracts.asset.Cash
import java.security.PublicKey
import java.time.Instant

// TODO We need to improve this contract code so it works with confidential identities.
class CampaignContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_REF = "net.corda.demos.crowdFunding.contracts.CampaignContract"
    }

    interface Commands : CommandData
    class Start : TypeOnlyCommandData(), Commands
    class End : TypeOnlyCommandData(), Commands
    class AcceptPledge : TypeOnlyCommandData(), Commands

    override fun verify(tx: LedgerTransaction) {
        val campaignCommand = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = campaignCommand.signers.toSet()

        when (campaignCommand.value) {
            is Start -> verifyStart(tx, setOfSigners)
            is End -> verifyEnd(tx, setOfSigners)
            is AcceptPledge -> verifyPledge(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyStart(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "No inputs should be consumed when starting a campaign." using (tx.inputStates.isEmpty())
        "Only one campaign state should be created when starting a campaign." using (tx.outputStates.size == 1)
        // There can only be one output state and it must be a Campaign state.
        val campaign = tx.outputStates.single() as Campaign

        // Assert stuff over the state.
        "A newly issued campaign must have a positive target." using
                (campaign.target > Amount(0, campaign.target.token))
        "A newly issued campaign must start with no pledges." using
                (campaign.raisedSoFar == Amount(0, campaign.target.token))
        "The deadline must be in the future." using (campaign.deadline > Instant.now())
        "There must be a campaign name." using (campaign.name != "")

        // Assert correct signers.
        "The campaign must be signed by the manager only." using (signers == keysFromParticipants(campaign))
    }

    private fun verifyPledge(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "The can only one input state in an accept pledge transaction." using (tx.inputStates.size == 1)
        "There must be two output states in an accept pledge transaction." using (tx.outputStates.size == 2)
        val campaignInput = tx.inputsOfType<Campaign>().single()
        val campaignOutput = tx.outputsOfType<Campaign>().single()
        val pledgeOutput = tx.outputsOfType<Pledge>().single()

        // Assert stuff about the pledge in relation to the campaign state.
        val changeInAmountRaised = campaignOutput.raisedSoFar - campaignInput.raisedSoFar
        "The pledge must be for this campaign." using (pledgeOutput.campaignReference == campaignOutput.linearId)
        "The campaign must be updated by the amount pledged." using (pledgeOutput.amount == changeInAmountRaised)

        // Assert stuff cannot change in the campaign state.
        "The campaign name may not change when accepting a pledge." using (campaignInput.name == campaignOutput.name)
        "The campaign deadline may not change when accepting a pledge." using
                (campaignInput.deadline == campaignOutput.deadline)
        "The campaign manager may not change when accepting a pledge." using
                (campaignInput.manager == campaignOutput.manager)
        "The campaign reference (linearId) may not change when accepting a pledge." using
                (campaignInput.linearId == campaignOutput.linearId)
        "The campaign target may not change when accepting a pledge." using
                (campaignInput.target == campaignOutput.target)

        // Assert that we can't make any pledges after the deadline.
        tx.timeWindow?.midpoint?.let {
            "No pledges can be accepted after the deadline." using (it < campaignOutput.deadline)
        } ?: throw IllegalArgumentException("A timestamp is required when pledging to a campaign.")

        // Assert correct signer.
        "The campaign must be signed by the manager only." using (signers.single() == campaignOutput.manager.owningKey)
    }

    private fun verifyEnd(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Assert we have the right amount and type of states.
        "Only one campaign can end per transaction." using (tx.inputsOfType<Campaign>().size == 1)
        "There must be no campaign output states when ending a campaign." using (tx.outputsOfType<Campaign>().isEmpty())
        "There must be no pledge output states when ending a campaign." using (tx.outputsOfType<Pledge>().isEmpty())
        // Get references to all the pledge and campaign states. Might have multiple or zero pledges, who knows?
        val campaignInput = tx.inputsOfType<Campaign>().single()
        val pledgeInputs = tx.inputsOfType<Pledge>()
        val cashInputs = tx.inputsOfType<Cash.State>()
        // Check there are states of no other types in this transaction.
        val totalInputStates = 1 + pledgeInputs.size + cashInputs.size
        "Un-required states have been added to this transaction." using (tx.inputs.size == totalInputStates)

        // Check we are not ending this campaign early.
        "The deadline must have passed before the campaign can be ended." using (campaignInput.deadline < Instant.now())

        // Check to see how many pledges we received.
        val zero = Amount.zero(campaignInput.target.token)
        val sumOfAllPledges = pledgeInputs.map { (amount) -> amount }.fold(zero) { acc, curr -> acc + curr }
        "There is a mismatch between input pledge states and Campaign.raisedSoFar" using
                (campaignInput.raisedSoFar == sumOfAllPledges)

        // do different stuff depending on how many pledges we get.
        when {
            sumOfAllPledges == zero -> verifyNoPledges(tx)
            sumOfAllPledges < campaignInput.target -> verifyMissedTarget(tx)
            sumOfAllPledges >= campaignInput.target -> verifyHitTarget(tx, campaignInput, pledgeInputs)
        }

        // Check the campaign state is signed by the campaign manager.
        "Ending campaign transactions must be signed by the campaign manager." using
                (campaignInput.manager.owningKey in signers)
    }

    private fun verifyNoPledges(tx: LedgerTransaction) {
        "No pledges were raised so there should be no pledge inputs." using (tx.inputsOfType<Pledge>().isEmpty())
        "No pledges were raised so there should be no cash inputs." using (tx.inputsOfType<Cash.State>().isEmpty())
        "No pledges were raised so there should be no cash outputs." using (tx.outputsOfType<Cash.State>().isEmpty())
        "There are disallowed input state types in this transaction." using (tx.inputs.size == 1)
        "There are disallowed output state types in this transaction." using (tx.outputs.isEmpty())
    }

    private fun verifyMissedTarget(tx: LedgerTransaction) {
        val pledges = tx.inputsOfType<Pledge>()
        "Pledges were raised so there should be pledge inputs." using (pledges.isNotEmpty())
        "Pledges were raised but we didn't hit the target so there should be no cash inputs." using
                (tx.inputsOfType<Cash.State>().isEmpty())
        "Pledges were raised but we didn't hit the target so there should be no cash outputs." using
                (tx.outputsOfType<Cash.State>().isEmpty())
        "There are disallowed input state types in this transaction." using (pledges.size + 1 == tx.inputs.size)
        "There are disallowed output state types in this transaction." using (tx.outputs.isEmpty())
    }

    private fun verifyHitTarget(tx: LedgerTransaction, campaign: Campaign, pledges: List<Pledge>) {
        "Pledges were raised so there should be pledge inputs." using (pledges.isNotEmpty())
        val cashOutputs = tx.outputsOfType<Cash.State>().sortedBy { it.amount.withoutIssuer() }
        val cashPaidToManager = cashOutputs.filter { it.owner == campaign.manager }.sortedBy { it.amount.withoutIssuer() }
        "There must be a payment for each pledge." using (cashOutputs.size == cashPaidToManager.size)

        // All the cash payments should match up with the pledges. No more, no less.
        // Zip kills multiple birds with one stone.
        // It ensures that number of output cash states paid to campaign manager == number of cancelled pledges.
        val matchedPayments = pledges.zip(cashPaidToManager) { pledge, cash -> pledge.amount == cash.amount.withoutIssuer() }
        "At least one of the cash payments is of an incorrect value. " using (matchedPayments.all { true })
        // The cash contract will assure amount of input cash == output cash and verify the correct signers.
    }
}