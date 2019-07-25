package negotiation.flows.contracts

import negotiation.contracts.ProposalAndTradeContract
import negotiation.states.ProposalState
import negotiation.states.TradeState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class ModificationContractTests {
    private val ledgerServices = MockServices(listOf("negotiation.contracts"))
    private val alice = TestIdentity(CordaX500Name("alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("bob", "Tokyo", "JP"))
    private val charlie = TestIdentity(CordaX500Name("charlie", "London", "GB"))

    @Test
    fun `proposal modification transactions have only one input and output`() {
        ledgerServices.ledger{
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                tweak {
                    input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                    input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                    fails()
                }
                tweak {

                    output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                    output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                    fails()
                }
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `proposal modification transactions have input of type Proposal State and output of type Proposal State`() {
        ledgerServices.ledger{
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                tweak {
                    input(ProposalAndTradeContract.ID, TradeState(1, alice.party, bob.party))
                    fails()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, DummyState())
                    fails()
                }
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `proposal modification transactions input and output should be of type ProposalState`() {
        ledgerServices.ledger{
            transaction {
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                tweak {
                    input(ProposalAndTradeContract.ID, TradeState(1, alice.party, bob.party))
                    fails()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, TradeState(1, alice.party, bob.party))
                    fails()
                }
                verifies()
            }
        }
    }

    @Test
    fun `proposal acceptance transactions have exactly one command of type Accept`() {
        ledgerServices.ledger {
            transaction {
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                tweak {
                    command(listOf(alice.publicKey, bob.publicKey), DummyCommandData)
                    fails()
                }
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                verifies()
            }
        }
    }

    @Test
    fun `proposal modification transactions have different amounts`() {
        ledgerServices.ledger{
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                tweak {
                    input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                    output(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                    fails()
                }
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `proposal modification transactions have two required signers - the proposer and the proposee`() {
        ledgerServices.ledger {
            transaction {
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                tweak {
                    command(listOf(alice.publicKey, charlie.publicKey), ProposalAndTradeContract.Commands.Modify())
                    fails()
                }
                tweak {
                    command(listOf(charlie.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                    fails()
                }
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                verifies()
            }
        }
    }

    @Test
    fun `The buyer and seller are unmodified in the output`() {
        ledgerServices.ledger {
            transaction {
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                tweak {
                    output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, charlie.party, alice.party, bob.party))
                    fails()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, ProposalState(2, charlie.party, bob.party, alice.party, bob.party))
                    fails()
                }
                tweak {
                    output(ProposalAndTradeContract.ID, ProposalState(2, bob.party, bob.party, alice.party, bob.party))
                    fails()
                }
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                verifies()
            }
        }
    }

    @Test
    fun `proposal acceptance transactions have no timestamp`() {
        ledgerServices.ledger {
            transaction {
                input(ProposalAndTradeContract.ID, ProposalState(1, alice.party, bob.party, alice.party, bob.party))
                output(ProposalAndTradeContract.ID, ProposalState(2, alice.party, bob.party, alice.party, bob.party))
                command(listOf(alice.publicKey, bob.publicKey), ProposalAndTradeContract.Commands.Modify())
                tweak {
                    timeWindow(Instant.now())
                    fails()
                }
            }
        }
    }
}