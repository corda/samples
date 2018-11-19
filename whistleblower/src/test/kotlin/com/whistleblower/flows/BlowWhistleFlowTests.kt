package com.whistleblower.flows

import com.whistleblower.BlowWhistleState
import net.corda.core.node.services.queryBy
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BlowWhistleFlowTests : FlowTestsBase() {

    @Test
    fun `flow completes successfully`() {
        blowWhistle()
    }

    @Test
    fun `both parties recorded transaction and state`() {
        val stx = blowWhistle()

        listOf(whistleBlower, firstInvestigator).forEach { node ->
            val recordedTx = node.services.validatedTransactions.getTransaction(stx.id)
            assertNotNull(recordedTx)
            val recordedStates = node.transaction {
                node.services.vaultService.queryBy<BlowWhistleState>().states
            }
            assertEquals(1, recordedStates.size)
            assertEquals(stx.id, recordedStates.single().ref.txhash)
        }
    }

    @Test
    fun `in created state, neither party is using a well-known identity`() {
        val stx = blowWhistle()

        val state = whistleBlower.transaction {
            stx.toLedgerTransaction(whistleBlower.services).outputsOfType<BlowWhistleState>().single()
        }

        assert(state.whistleBlower.owningKey !in whistleBlower.legalIdentityKeys)
        assert(state.investigator.owningKey !in firstInvestigator.legalIdentityKeys)
    }

    @Test
    fun `parties have exchanged certs linking confidential IDs to well-known IDs`() {
        val stx = blowWhistle()

        val state = whistleBlower.transaction {
            stx.toLedgerTransaction(whistleBlower.services).outputsOfType<BlowWhistleState>().single()
        }

        whistleBlower.transaction {
            assertNotNull(whistleBlower.partyFromAnonymous(state.investigator))
        }
        firstInvestigator.transaction {
            assertNotNull(firstInvestigator.partyFromAnonymous(state.whistleBlower))
        }
    }

    @Test
    fun `third-party cannot link the confidential IDs to well-known IDs`() {
        val stx = blowWhistle()

        val state = whistleBlower.transaction {
            stx.toLedgerTransaction(whistleBlower.services).outputsOfType<BlowWhistleState>().single()
        }

        badCompany.transaction {
            listOf(state.investigator, state.whistleBlower).forEach {
                assertNull(badCompany.partyFromAnonymous(it))
            }
        }
    }
}