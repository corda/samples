package net.corda.yo

import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.contracts.DummyContract
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class YoFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.yo"))))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun flowWorksCorrectly() {
        val yo = YoState(a.info.legalIdentities.first(), b.info.legalIdentities.first())
        val flow = YoFlow(b.info.legalIdentities.first())
        val future = a.startFlow(flow)
        network.runNetwork()
        val stx = future.getOrThrow()
        // Check yo transaction is stored in the storage service.
        val bTx = b.services.validatedTransactions.getTransaction(stx.id)
        assertEquals(bTx, stx)
        println("bTx == $stx")
        // Check yo state is stored in the vault.
        b.transaction {
            // Simple query.
            val bYo = b.services.vaultService.queryBy<YoState>().states.single().state.data
            assertEquals(bYo.toString(), yo.toString())
            println("$bYo == $yo")
        }
    }
}

class YoContractTests {
    private val ledgerServices = MockServices(listOf("net.corda.yo", "net.corda.testing.contracts"))
    private val alice = TestIdentity(CordaX500Name("Alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("Bob", "Tokyo", "JP"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun yoTransactionMustBeWellFormed() {
        // A pre-made Yo to Bob.
        val yo = YoState(alice.party, bob.party)
        // Tests.
        ledgerServices.ledger {
            // Input state present.
            transaction {
                input(DummyContract.PROGRAM_ID, DummyState())
                command(alice.publicKey, YoContract.Send())
                output(YoContract.ID, yo)
                this.failsWith("There can be no inputs when Yo'ing other parties.")
            }
            // Wrong command.
            transaction {
                output(YoContract.ID, yo)
                command(alice.publicKey, DummyCommandData)
                this.failsWith("")
            }
            // Command signed by wrong key.
            transaction {
                output(YoContract.ID, yo)
                command(miniCorp.publicKey, YoContract.Send())
                this.failsWith("The Yo! must be signed by the sender.")
            }
            // Sending to yourself is not allowed.
            transaction {
                output(YoContract.ID, YoState(alice.party, alice.party))
                command(alice.publicKey, YoContract.Send())
                this.failsWith("No sending Yo's to yourself!")
            }
            transaction {
                output(YoContract.ID, yo)
                command(alice.publicKey, YoContract.Send())
                this.verifies()
            }
        }
    }
}