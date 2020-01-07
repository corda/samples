package com.example.contract

import com.example.contract.SanctionableIOUContract.Companion.IOU_CONTRACT_ID
import com.example.contract.SanctionedEntitiesContract.Companion.SANCTIONS_CONTRACT_ID
import com.example.state.SanctionableIOUState
import com.example.state.SanctionedEntities
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NotaryInfo
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class IOUContractTests {
    val DUMMY_NOTARY = TestIdentity(DUMMY_NOTARY_NAME, 20).party

    private val issuer = TestIdentity(CordaX500Name("SanctionsIssuer", "London", "GB"))
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val naughtyCorp = TestIdentity(CordaX500Name("NaughtyCorp", "Moscow", "RU"))

    private val ledgerServices = MockServices(
        megaCorp,
        networkParameters = testNetworkParameters(
            minimumPlatformVersion = 4,
            notaries = listOf(NotaryInfo(DUMMY_NOTARY, true))
        )
    )

    private val sanctions = SanctionedEntities(listOf(naughtyCorp.party), issuer.party)

    private val iouValue = 1


    @Test
    fun `transaction must include reference sanctions list command`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                command(
                    listOf(megaCorp.publicKey, miniCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                fails()
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                verifies()
            }
        }
    }

    @Test
    fun `should not allow lender to be sanctioned`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, naughtyCorp.party, megaCorp.party))
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                command(
                    listOf(naughtyCorp.publicKey, megaCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                `fails with`("The lender O=NaughtyCorp, L=Moscow, C=RU is a sanctioned entity")
            }
        }
    }

    @Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                fails()
                command(
                    listOf(megaCorp.publicKey, miniCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                input(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                command(
                    listOf(megaCorp.publicKey, miniCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                command(
                    listOf(megaCorp.publicKey, miniCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, SanctionableIOUContract.Commands.Create(issuer.party))
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, SanctionableIOUContract.Commands.Create(issuer.party))
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        ledgerServices.ledger {
            transaction {
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                output(IOU_CONTRACT_ID, SanctionableIOUState(iouValue, megaCorp.party, megaCorp.party))
                command(
                    listOf(megaCorp.publicKey, miniCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        ledgerServices.ledger {
            transaction {
                reference(SANCTIONS_CONTRACT_ID, sanctions)
                output(IOU_CONTRACT_ID, SanctionableIOUState(-1, miniCorp.party, megaCorp.party))
                command(
                    listOf(megaCorp.publicKey, miniCorp.publicKey),
                    SanctionableIOUContract.Commands.Create(issuer.party)
                )
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}