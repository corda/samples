package negotiation.flows.contracts

import negotiation.contracts.ProposalAndTradeContract
import negotiation.states.ProposalState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyContract
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class ProposalContractTests {
    private val ledgerServices = MockServices(listOf("negotiation.contracts", "net.corda.testing.contracts"))
    private val alice = TestIdentity(CordaX500Name("alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("bob", "Tokyo", "JP"))
    private val charlie = TestIdentity(CordaX500Name("charlie", "London", "GB"))

    @Test
    fun `proposal transactions have exactly one output of type ProposalState`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Propose())
                fails()
                tweak {
                    output(ProposalAndTradeContract.ID, DummyState())
                    fails()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                    output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                    fails()
                }
                output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `proposal transactions have exactly one command of type Propose`() {
        ledgerServices.ledger {
            transaction {
                output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                tweak {
                    command(listOf(alice.publicKey, bob.publicKey), DummyCommandData)
                    fails()
                }
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Propose())
                verifies()
            }
        }
    }

    @Test
    fun `proposal transactions have two required signers - the proposer and the proposee`() {
        ledgerServices.ledger {
            transaction {
                output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                tweak {
                    command(listOf(alice.publicKey, charlie.publicKey), ProposalAndTradeContract.Commands.Propose())
                    fails()
                }
                tweak {
                    command(listOf(charlie.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Propose())
                    fails()
                }
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Propose())
                verifies()
            }
        }
    }

    @Test
    fun `in proposal transactions, the proposer and proposee are the buyer and seller`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Propose())
                tweak {
                    // Order reversed - buyer = proposee, seller = proposer
                    output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, bob.party, alice.party))
                    verifies()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, ProposalState(1, charlie.party, bob.party, alice.party, bob.party))
                    fails()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, charlie.party, alice.party, bob.party))
                    fails()
                }
                output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `proposal transactions have no inputs and no timestamp`() {
        ledgerServices.ledger {
            transaction {
                output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Propose())
                tweak {
                    input(DummyContract.PROGRAM_ID, DummyState())
                    fails()
                }
                tweak {
                    timeWindow(Instant.now())
                    fails()
                }
            }
        }
    }
}