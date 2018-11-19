package net.corda.demos.crowdFunding.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.hours
import net.corda.core.utilities.seconds
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import net.corda.finance.POUNDS
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

// TODO: Write more tests.
class PledgeTests {
    private val ledgerServices = MockServices(listOf("net.corda.testing.contracts", "net.corda.demos.crowdFunding", "net.corda.finance"))
    private val A = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val B = TestIdentity(CordaX500Name("Bob", "", "GB"))

    private fun partyKeys(vararg identities: TestIdentity) = identities.map { it.party.owningKey }
    private val oneHourFromNow = Instant.now() + 1.hours

    private val newValidCampaign = Campaign(
            name = "Roger's Campaign",
            manager = A.party,
            target = 1000.POUNDS,
            deadline = oneHourFromNow
    )

    @Test
    fun `Make pledge tests`() {
        val defaultPledge = Pledge(100.POUNDS, B.party, A.party, newValidCampaign.linearId)

        ledgerServices.ledger {
            // Valid make pledge transaction.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = newValidCampaign.raisedSoFar + defaultPledge.amount))
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A, B), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.verifies()
            }

            // Pledging a zero amount.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy())
                output(PledgeContract.CONTRACT_REF, defaultPledge.copy(amount = 0.POUNDS))
                command(partyKeys(A, B), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Creating more than one pledge at a time.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = newValidCampaign.raisedSoFar + defaultPledge.amount))
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A, B), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // States in the wrong place.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 200.POUNDS))
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                input(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A, B), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Missing campaign states.
            transaction {
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A, B), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Incorrect signers.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = newValidCampaign.raisedSoFar + defaultPledge.amount))
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = newValidCampaign.raisedSoFar + defaultPledge.amount))
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), PledgeContract.Create())
                command(partyKeys(A), CampaignContract.AcceptPledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }
        }

    }

    @Test
    fun `Cancel pledge tests`() {
        val defaultPledge = Pledge(100.POUNDS, B.party, A.party, newValidCampaign.linearId)

        val endedCampaign = newValidCampaign.copy(
                deadline = Instant.now().minusSeconds(1),
                raisedSoFar = 100.POUNDS)

        ledgerServices.ledger {
            // Valid make pledge transaction.
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                input(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A), PledgeContract.Cancel())
                command(partyKeys(A), CampaignContract.End())
                this.verifies()
            }

            // Has pledge outputs.
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                input(PledgeContract.CONTRACT_REF, defaultPledge)
                output(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A, B), PledgeContract.Cancel())
                command(partyKeys(A), CampaignContract.End())
                this.fails()
            }

            // Wrong public key.
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                input(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), PledgeContract.Cancel())
                command(partyKeys(A), CampaignContract.End())
                this.fails()
            }

            // No campaign state present.
            transaction {
                input(PledgeContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), PledgeContract.Cancel())
                command(partyKeys(A), CampaignContract.End())
                this.fails()
            }

            // Cancelling a pledge for a different campaign.
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                input(PledgeContract.CONTRACT_REF, defaultPledge.copy(campaignReference = UniqueIdentifier()))
                command(partyKeys(A), PledgeContract.Cancel())
                command(partyKeys(A), CampaignContract.End())
                this.fails()
            }
        }
    }

}