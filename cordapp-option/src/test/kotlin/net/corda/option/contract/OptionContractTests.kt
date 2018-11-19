package net.corda.option.contract

import net.corda.finance.DOLLARS
import net.corda.finance.contracts.asset.CASH
import net.corda.finance.contracts.asset.Cash
import net.corda.option.MEGA_CORP
import net.corda.option.MINI_CORP
import net.corda.option.ORACLE
import net.corda.option.base.KNOWN_SPOTS
import net.corda.option.base.KNOWN_VOLATILITIES
import net.corda.option.base.contract.OptionContract
import net.corda.option.base.contract.OptionContract.Companion.OPTION_CONTRACT_ID
import net.corda.option.base.state.OptionState
import net.corda.option.createOption
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Duration
import java.time.Instant

class OptionContractTests {
    private val ledgerServices = MockServices(listOf("net.corda.option.base.contract", "net.corda.finance.contracts.asset"))

    // TODO: These tests only test the golden path. Many more tests could be added to test various contract violations
    // TODO: (e.g. insufficient cash, bad oracle data, etc.)
    @Test
    fun `transaction tests`() {
        val issuer = MEGA_CORP.ref(123)
        val option = createOption(MEGA_CORP.party, MINI_CORP.party)
        option.spotPriceAtPurchase = 3.DOLLARS
        // By the point of exercise, the option has already been transferred.
        val exercisedOption = option.copy(owner = MEGA_CORP.party, exercised = true, exercisedOnDate = Instant.now())

        ledgerServices.ledger {
            unverifiedTransaction("Issue $9 to Mini Corp") {
                output(Cash.PROGRAM_ID, "Mini Corp's $9", 9.DOLLARS.CASH.issuedBy(issuer).ownedBy(MINI_CORP.party))
            }

            transaction("Mega Corp issues an option to Mini Corp in exchange for $9") {
                input("Mini Corp's $9")
                output(OPTION_CONTRACT_ID, "Mini Corp's option", option)
                output(Cash.PROGRAM_ID, "Mega Corp's $9", 9.DOLLARS.CASH.issuedBy(issuer).ownedBy(MEGA_CORP.party))
                command(listOf(MEGA_CORP.publicKey, MINI_CORP.publicKey), OptionContract.Commands.Issue())
                command(ORACLE.publicKey, OptionContract.OracleCommand(KNOWN_SPOTS[0], KNOWN_VOLATILITIES[0]))
                command(MINI_CORP.publicKey, Cash.Commands.Move())
                timeWindow(Instant.now(), Duration.ofSeconds(60))
                verifies()
            }

            transaction("Mini Corp sells the option back to Mega Corp for $9") {
                input("Mini Corp's option")
                input("Mega Corp's $9")
                output(OPTION_CONTRACT_ID, "Mega Corp's option", "Mini Corp's option".output<OptionState>().copy(owner = MEGA_CORP.party))
                output(Cash.PROGRAM_ID, "Mini Corp's new $9", 9.DOLLARS.CASH.issuedBy(issuer).ownedBy(MINI_CORP.party))
                command(listOf(MEGA_CORP.publicKey, MINI_CORP.publicKey), OptionContract.Commands.Trade())
                command(ORACLE.publicKey, OptionContract.OracleCommand(KNOWN_SPOTS[0], KNOWN_VOLATILITIES[0]))
                command(MEGA_CORP.publicKey, Cash.Commands.Move())
                timeWindow(Instant.now(), Duration.ofSeconds(60))
                verifies()
            }

            transaction("Mega Corp exercises its option") {
                input("Mega Corp's option")
                output(OPTION_CONTRACT_ID, "Mega Corp's exercised option", exercisedOption)
                command(MEGA_CORP.publicKey, OptionContract.Commands.Exercise())
                timeWindow(Instant.now(), Duration.ofSeconds(60))
                verifies()
            }
        }
    }
}