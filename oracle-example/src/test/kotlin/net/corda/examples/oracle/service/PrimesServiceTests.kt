package net.corda.examples.oracle.service

import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import net.corda.examples.oracle.base.contract.PRIME_PROGRAM_ID
import net.corda.examples.oracle.base.contract.PrimeContract
import net.corda.examples.oracle.base.contract.PrimeState
import net.corda.examples.oracle.service.service.Oracle
import net.corda.testing.core.SerializationEnvironmentRule
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService
import org.junit.Rule
import org.junit.Test
import java.util.function.Predicate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrimesServiceTests {
    private val oracleIdentity = TestIdentity(CordaX500Name("Oracle", "New York", "US"))
    private val dummyServices = MockServices(listOf("net.corda.examples.oracle.base.contract"), oracleIdentity)
    private val oracle = Oracle(dummyServices)
    private val aliceIdentity = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val notaryIdentity = TestIdentity(CordaX500Name("Notary", "", "GB"))

    @Rule
    @JvmField
    val testSerialization = SerializationEnvironmentRule()

    @Test
    fun `oracle returns correct Nth prime`() {
        assertEquals(104729, oracle.query(10000))
    }

    @Test
    fun `oracle rejects invalid values of N`() {
        assertFailsWith<IllegalArgumentException> { oracle.query(0) }
        assertFailsWith<IllegalArgumentException> { oracle.query(-1) }
    }

    @Test
    fun `oracle signs transactions including a valid prime`() {
        val command = Command(PrimeContract.Create(10, 29), listOf(oracleIdentity.publicKey))
        val state = PrimeState(10, 29, aliceIdentity.party)
        val stateAndContract = StateAndContract(state, PRIME_PROGRAM_ID)
        val ftx = TransactionBuilder(notaryIdentity.party)
                .withItems(stateAndContract, command)
                .toWireTransaction(dummyServices)
                .buildFilteredTransaction(Predicate {
                    when (it) {
                        is Command<*> -> oracle.services.myInfo.legalIdentities.first().owningKey in it.signers && it.value is PrimeContract.Create
                        else -> false
                    }
                })
        val signature = oracle.sign(ftx)
        assert(signature.verify(ftx.id))
    }

    @Test
    fun `oracle does not sign transactions including an invalid prime`() {
        val command = Command(PrimeContract.Create(10, 1000), listOf(oracleIdentity.publicKey))
        val state = PrimeState(10, 29, aliceIdentity.party)
        val stateAndContract = StateAndContract(state, PRIME_PROGRAM_ID)
        val ftx = TransactionBuilder(notaryIdentity.party)
                .withItems(stateAndContract, command)
                .toWireTransaction(oracle.services)
                .buildFilteredTransaction(Predicate {
                    when (it) {
                        is Command<*> -> oracle.services.myInfo.legalIdentities.first().owningKey in it.signers && it.value is PrimeContract.Create
                        else -> false
                    }
                })
        assertFailsWith<IllegalArgumentException> { oracle.sign(ftx) }
    }
}