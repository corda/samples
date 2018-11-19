package net.corda.examples.oracle.client

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.examples.oracle.base.contract.PrimeState
import net.corda.examples.oracle.client.flow.CreatePrime
import net.corda.examples.oracle.service.flow.QueryHandler
import net.corda.examples.oracle.service.flow.SignHandler
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PrimesClientTests {
    private val mockNet = MockNetwork(listOf("net.corda.examples.oracle.service.service", "net.corda.examples.oracle.base.contract"))
    private lateinit var a: StartedMockNode

    @Before
    fun setUp() {
        a = mockNet.createNode()

        val oracleName = CordaX500Name("Oracle", "New York", "US")
        val oracle = mockNet.createNode(MockNodeParameters(legalName = oracleName))
        listOf(QueryHandler::class.java, SignHandler::class.java).forEach { oracle.registerInitiatedFlow(it) }

        mockNet.runNetwork()
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    @Test
    fun `oracle returns correct Nth prime`() {
        val flow = a.startFlow(CreatePrime(100))
        mockNet.runNetwork()
        val result = flow.getOrThrow().tx.outputsOfType<PrimeState>().single()
        assertEquals(100, result.n)
        val prime100 = 541
        assertEquals(prime100, result.nthPrime)
    }

}