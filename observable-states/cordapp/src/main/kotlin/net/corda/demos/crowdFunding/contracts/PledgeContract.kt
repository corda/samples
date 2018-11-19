package net.corda.demos.crowdFunding.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.demos.crowdFunding.keysFromParticipants
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import java.security.PublicKey

class PledgeContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_REF = "net.corda.demos.crowdFunding.contracts.PledgeContract"
    }

    interface Commands : CommandData
    class Create : TypeOnlyCommandData(), Commands
    class Cancel : TypeOnlyCommandData(), Commands
    class Update : TypeOnlyCommandData(), Commands // TODO: Update pledge.

    override fun verify(tx: LedgerTransaction) {
        // We only need the pledge commands at this point to determine which part of the contract code to run.
        val pledgeCommand = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = pledgeCommand.signers.toSet()

        when (pledgeCommand.value) {
            is Create -> verifyCreate(tx, setOfSigners)
            is Cancel -> verifyCancel(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Group pledges by campaign id.
        val pledgeStates = tx.groupStates(Pledge::class.java, { it.linearId })
        "You can only create one pledge at a time." using (pledgeStates.size == 1)
        val campaignStates = tx.groupStates(Campaign::class.java, { it.linearId })
        "A Pledge can only be created if there are campaign states present." using (campaignStates.isNotEmpty())

        // Assert we have the right amount and type of states.
        val pledgeStatesGroup = pledgeStates.single()
        "No inputs should be consumed when creating a pledge." using (pledgeStatesGroup.inputs.isEmpty())
        "Only one campaign state should be created when starting a campaign." using (pledgeStatesGroup.outputs.size == 1)
        val pledge = pledgeStatesGroup.outputs.single()

        // Assert stuff over the state.
        "You cannot pledge a zero amount." using (pledge.amount > Amount(0, pledge.amount.token))

        // Assert correct signers.
        "The campaign must be signed by the manager and the pledger." using (signers == keysFromParticipants(pledge))
    }

    private fun verifyCancel(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Group pledges by linear id.
        val pledgeGroups = tx.groupStates(Pledge::class.java, { it.linearId })

        // Check that there is a campaign state present. If there is then the campaign contract code will be run as well.
        "A Pledge can only be cancelled if there is a campaign input state present." using
                (tx.inputsOfType<Campaign>().size == 1)
        val campaign = tx.inputsOfType<Campaign>().single()

        // Verify each pledge separately.
        pledgeGroups.forEach { (inputs, outputs) ->
            // Check there's only one output per group.
            "No outputs should be created when cancelling a pledge." using (outputs.isEmpty())
            "There should be no duplicate pledge states." using (inputs.size == 1)
            val pledge = inputs.single()
            "You are cancelling a pledge for a different campaign!" using (pledge.campaignReference == campaign.linearId)

            // Assert correct signers (Only the campaign manager can cancel a pledge).
            "The cancel pledge transaction must be signed by the campaign manager of the campaign the pledge is for." using
                    (campaign.manager.owningKey == signers.single())
        }
    }
}