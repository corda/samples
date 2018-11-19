package com.whistleblower.contract

import com.whistleblower.BLOW_WHISTLE_CONTRACT_ID
import com.whistleblower.BlowWhistleContract.Commands.BlowWhistleCmd
import com.whistleblower.BlowWhistleState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class BlowWhistleTests {
    private val ledgerServices = MockServices(listOf("com.whistleblower"))
    private val badCompany = TestIdentity(CordaX500Name("Bad Company", "Eldoret", "KE")).party
    private val whistleBlower = TestIdentity(CordaX500Name("Whistle Blower", "Nairobi", "KE")).party.anonymise()
    private val investigator = TestIdentity(CordaX500Name("Investigator", "Kisumu", "KE")).party.anonymise()

    @Test
    fun `A BlowWhistleState transaction must have a BlowWhistleContract command`() {
        // Wrong command type.
        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), DummyCommandData)
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                verifies()
            }
        }
    }

    @Test
    fun `A BlowWhistle transaction should have zero inputs and a single BlowWhistleState output`() {
        // Input state.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                fails()
            }
        }
        // Wrong output state type.
        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, DummyState(0))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                fails()
            }
        }
        // Two output states.
        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                verifies()
            }
        }
    }

    @Test
    fun `A BlowWhistle transaction should be signed by the whistle-blower and the investigator`() {
        // No whistle-blower signature.
        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(investigator.owningKey, BlowWhistleCmd())
                fails()
            }
        }
        // No investigator signature.
        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(whistleBlower.owningKey, BlowWhistleCmd())
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                verifies()
            }
        }
    }
}