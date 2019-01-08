package net.corda.demos.crowdFunding.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.hours
import net.corda.core.utilities.seconds
import net.corda.demos.crowdFunding.structures.Campaign
import net.corda.demos.crowdFunding.structures.Pledge
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.contracts.DummyContract
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class CampaignTests {
    private val ledgerServices = MockServices(listOf("net.corda.testing.contracts", "net.corda.demos.crowdFunding", "net.corda.finance"))
    private val issuer = TestIdentity(CordaX500Name("Issuer", "", "GB"))
    private val A = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val B = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val C = TestIdentity(CordaX500Name("Carl", "", "GB"))
    private val D = TestIdentity(CordaX500Name("Demi", "", "GB"))

    private val defaultIssuer = issuer.ref(Byte.MIN_VALUE)
    private fun partyKeys(vararg identities: TestIdentity) = identities.map { it.party.owningKey }
    private val oneHourFromNow = Instant.now() + 1.hours

    private val newValidCampaign = Campaign(
            name = "Roger's Campaign",
            manager = A.party,
            target = 1000.POUNDS,
            deadline = oneHourFromNow
    )

    @Test
    fun `Start new campaign tests`() {
        ledgerServices.ledger {
            // Valid start new campaign transaction.
            transaction {
                output(CampaignContract.CONTRACT_REF, newValidCampaign)
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.verifies()
            }

            // Signers incorrect.
            transaction {
                output(CampaignContract.CONTRACT_REF, newValidCampaign)
                command(partyKeys(A, B), CampaignContract.Commands.Start())
                this.fails()
            }

            // Incorrect inputs / outputs.
            transaction {
                input(DummyContract.PROGRAM_ID, DummyState())
                output(CampaignContract.CONTRACT_REF, newValidCampaign)
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.fails()
            }

            // Incorrect inputs / outputs.
            transaction {
                output(DummyContract.PROGRAM_ID, DummyState())
                output(CampaignContract.CONTRACT_REF, newValidCampaign)
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.fails()
            }

            // Zero amount.
            transaction {
                output(CampaignContract.CONTRACT_REF, Campaign("Test", A.party, 0.POUNDS, oneHourFromNow))
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.fails()
            }

            // Deadline not in future.
            transaction {
                output(CampaignContract.CONTRACT_REF, Campaign("Test", A.party, 0.POUNDS, Instant.now()))
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.fails()
            }

            // No name.
            transaction {
                output(CampaignContract.CONTRACT_REF, Campaign("", A.party, 0.POUNDS, Instant.now()))
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.fails()
            }

            // Raised so far, not zero.
            transaction {
                output(CampaignContract.CONTRACT_REF, Campaign("Test", A.party, 0.POUNDS, Instant.now(), 10.POUNDS))
                command(partyKeys(A), CampaignContract.Commands.Start())
                this.fails()
            }
        }
    }

    @Test
    fun `Make a pledge tests`() {
        val defaultPledge = Pledge(100.POUNDS, B.party, A.party, newValidCampaign.linearId)

        ledgerServices.ledger {
            // Valid make pledge transaction.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = newValidCampaign.raisedSoFar + defaultPledge.amount))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.verifies()
            }

            // Amounts don't match up.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 200.POUNDS)) // Wrong amount.
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Wrong currency.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign.copy(target = 1000.DOLLARS))
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 200.DOLLARS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Missing output.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Missing input.
            transaction {
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Additional irrelevant states.
            transaction {
                input(DummyContract.PROGRAM_ID, DummyState())
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Additional irrelevant states
            transaction {
                output(DummyContract.PROGRAM_ID, DummyState())
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Changing campaign stuff that shouldn't change.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS, name = "Changed"))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Changing campaign stuff that shouldn't change.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS, target = 200.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Changing campaign stuff that shouldn't change.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS, linearId = UniqueIdentifier()))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Changing campaign stuff that shouldn't change.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS, deadline = Instant.now()))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Pledge for wrong campaign.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge.copy(campaignReference = UniqueIdentifier())) // Wrong campaign reference.
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }

            // Pledge after deadline.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(B), CampaignContract.Commands.Pledge())
                timeWindow(newValidCampaign.deadline + 1.seconds, 5.seconds)
                this.fails()
            }

            // Wrong key in pledge command.
            transaction {
                input(CampaignContract.CONTRACT_REF, newValidCampaign)
                output(CampaignContract.CONTRACT_REF, newValidCampaign.copy(raisedSoFar = 100.POUNDS))
                output(CampaignContract.CONTRACT_REF, defaultPledge)
                command(partyKeys(A), CampaignContract.Commands.Pledge())
                timeWindow(Instant.now(), 5.seconds)
                this.fails()
            }
        }
    }

    @Test
    fun `End campaign after failure with no pledges`() {
        val pledge = Pledge(100.POUNDS, B.party, A.party, newValidCampaign.linearId)
        val endedCampaign = newValidCampaign.copy(deadline = Instant.now().minusSeconds(1))

        ledgerServices.ledger {
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                command(partyKeys(A), CampaignContract.Commands.End())
                this.verifies()
            }

            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                input(CampaignContract.CONTRACT_REF, pledge)
                command(partyKeys(A), CampaignContract.Commands.End())
                this.fails()
            }
        }
    }

    @Test
    fun `End campaign after failure with some pledges`() {
        val pledge = Pledge(100.POUNDS, B.party, A.party, newValidCampaign.linearId)
        val endedCampaign = newValidCampaign.copy(deadline = Instant.now().minusSeconds(1))

        ledgerServices.ledger {
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign.copy(raisedSoFar = 100.POUNDS))
                input(CampaignContract.CONTRACT_REF, pledge)
                command(partyKeys(A), CampaignContract.Commands.End())
                this.verifies()
            }

            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign.copy(raisedSoFar = 500.POUNDS))
                input(CampaignContract.CONTRACT_REF, pledge)
                input(CampaignContract.CONTRACT_REF, pledge.copy(linearId = UniqueIdentifier()))
                input(CampaignContract.CONTRACT_REF, pledge.copy(linearId = UniqueIdentifier()))
                input(CampaignContract.CONTRACT_REF, pledge.copy(linearId = UniqueIdentifier()))
                input(CampaignContract.CONTRACT_REF, pledge.copy(linearId = UniqueIdentifier()))
                command(partyKeys(A), CampaignContract.Commands.End())
                this.verifies()
            }

            // Wrong campaign.
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign.copy(raisedSoFar = 100.POUNDS))
                input(CampaignContract.CONTRACT_REF, pledge.copy(campaignReference = UniqueIdentifier()))
                command(partyKeys(A), CampaignContract.Commands.End())
                this.fails()
            }

            // Raised vs pledge mismatch.
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign.copy(raisedSoFar = 100.POUNDS))
                command(partyKeys(A), CampaignContract.Commands.End())
                this.fails()
            }
        }
    }

    @Test
    fun `End campaign in success`() {
        val endedCampaign = newValidCampaign.copy(
                deadline = Instant.now().minusSeconds(1),
                raisedSoFar = 1000.POUNDS)

        ledgerServices.ledger {
            transaction {
                input(CampaignContract.CONTRACT_REF, endedCampaign)
                input(CampaignContract.CONTRACT_REF, Pledge(500.POUNDS, B.party, A.party, endedCampaign.linearId))
                input(CampaignContract.CONTRACT_REF, Pledge(200.POUNDS, D.party, A.party, endedCampaign.linearId))
                input(CampaignContract.CONTRACT_REF, Pledge(300.POUNDS, C.party, A.party, endedCampaign.linearId))
                input(Cash.PROGRAM_ID, Cash.State(defaultIssuer, 500.POUNDS, B.party))
                input(Cash.PROGRAM_ID, Cash.State(defaultIssuer, 300.POUNDS, C.party))
                input(Cash.PROGRAM_ID, Cash.State(defaultIssuer, 200.POUNDS, D.party))
                output(Cash.PROGRAM_ID, Cash.State(defaultIssuer, 500.POUNDS, A.party))
                output(Cash.PROGRAM_ID, Cash.State(defaultIssuer, 300.POUNDS, A.party))
                output(Cash.PROGRAM_ID, Cash.State(defaultIssuer, 200.POUNDS, A.party))
                command(partyKeys(A), CampaignContract.Commands.End())
                command(partyKeys(B, C, D), Cash.Commands.Move())
                this.verifies()
            }
        }
    }
}