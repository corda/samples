package com.upgrade.client

import com.upgrade.cordapp.Initiator
import com.upgrade.legacy.constraint.new.NewContractWithLegacyConstraint
import com.upgrade.new.NewContract
import com.upgrade.new.NewState
import com.upgrade.old.OldContract
import com.upgrade.old.OldState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.crypto.sha256
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.readFully
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import net.corda.testing.node.internal.findCordapp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.FileInputStream

class ContractUpgradeTest {

  private val bankA = TestIdentity(CordaX500Name("PartyA", "", "GB"))
  private val bankB = TestIdentity(CordaX500Name("PartyB", "", "US"))

  @Test
  fun shouldBeAbleToUpgradeUpgradeContract() {
    val whitelist = mapOf("com.upgrade.old.OldContract" to "com.upgrade.old", "com.upgrade.new.NewContract" to "com.upgrade.new")
        .map{ it.key to listOf(FileInputStream(findCordapp(it.value).jarFile.toString()).readFully().sha256())}.toMap()

    val networkParams = testNetworkParameters(whitelistedContractImplementations = whitelist)

    driver(DriverParameters(isDebug = true, startNodesInProcess = true, networkParameters = networkParams, cordappsForAllNodes = listOf(findCordapp("com.upgrade.old"), findCordapp("com.upgrade.new"), findCordapp("com.upgrade.cordapp")))) {

      val rpcUser = User("user", "test", setOf("ALL"))

      val (partyAProxy, partyBProxy) = listOf(bankA, bankB)
          .map { startNode(providedName = it.name, rpcUsers = listOf(rpcUser)) }
          .map { it.getOrThrow() }
          .map {
            CordaRPCClient(it.rpcAddress).start("user","test").proxy
          }

      // Issue a State that uses OldContract onto the ledger.
      val partyBIdentity = partyBProxy.nodeInfo().legalIdentities.first()
      partyAProxy.startFlowDynamic(Initiator::class.java, partyBIdentity).returnValue.get()

      val oldStates = partyAProxy.vaultQuery(OldState::class.java).states
      assertEquals("there should be only one old state on partyA", 1, oldStates.size)

      // Authorise the upgrading of all the State instances using OldContract.
      listOf(partyBProxy).forEach { proxy ->
        // Extract all the unconsumed State instances from the vault.
        val stateAndRefs = proxy.vaultQuery(OldState::class.java).states
        assertEquals("there should be only one old state on partyB", 1, oldStates.size)

        // Run the upgrading flow for each one.
        stateAndRefs
            .filter { stateAndRef ->
              stateAndRef.state.contract == OldContract.id
            }.forEach { stateAndRef ->
              proxy.startFlowDynamic(
                  ContractUpgradeFlow.Authorise::class.java,
                  stateAndRef,
                  NewContract::class.java).returnValue.get()
            }
      }

      // Initiate the upgrading of all the State instances using OldContract.
      partyAProxy.vaultQuery(OldState::class.java).states
          .filter { stateAndRef ->
            stateAndRef.state.contract == OldContract.id
          }
          .map { stateAndRef ->
            partyAProxy.startFlowDynamic(
                ContractUpgradeFlow.Initiate::class.java,
                stateAndRef,
                NewContract::class.java)
          }.map {
            it.returnValue.getOrThrow()
          }

      // Log all the State instances in the vault to show they are using NewContract.
      val newPartyAStates = partyAProxy.vaultQuery(NewState::class.java).states
      assertEquals("there should be one new state on party a", 1, newPartyAStates.size)
    }
  }

  @Test
  fun shouldBeAbleToUpgradeUpgradeContractWithLegacyConstraint() {
    driver(DriverParameters(isDebug = true, startNodesInProcess = true, cordappsForAllNodes = listOf(findCordapp("com.upgrade.old"), findCordapp("com.upgrade.legacy.constraint.new"), findCordapp("com.upgrade.cordapp")))) {

      val rpcUser = User("user", "test", setOf("ALL"))

      val (partyAProxy, partyBProxy) = listOf(bankA, bankB)
          .map { startNode(providedName = it.name, rpcUsers = listOf(rpcUser)) }
          .map { it.getOrThrow() }
          .map {
            CordaRPCClient(it.rpcAddress).start("user","test").proxy
          }

      // Issue a State that uses OldContract onto the ledger.
      val partyBIdentity = partyBProxy.nodeInfo().legalIdentities.first()
      partyAProxy.startFlowDynamic(Initiator::class.java, partyBIdentity).returnValue.get()

      val oldStates = partyAProxy.vaultQuery(OldState::class.java).states
      assertEquals("there should be only one old state on partyA", 1, oldStates.size)

      // Authorise the upgrading of all the State instances using OldContract.
      listOf(partyBProxy).forEach { proxy ->
        // Extract all the unconsumed State instances from the vault.
        val stateAndRefs = proxy.vaultQuery(OldState::class.java).states
        assertEquals("there should be only one old state on partyB", 1, oldStates.size)

        // Run the upgrading flow for each one.
        stateAndRefs
            .filter { stateAndRef ->
              stateAndRef.state.contract == OldContract.id
            }.map { stateAndRef ->
              proxy.startFlowDynamic(
                  ContractUpgradeFlow.Authorise::class.java,
                  stateAndRef,
                  NewContractWithLegacyConstraint::class.java).returnValue.get()
            }
      }

      // Initiate the upgrading of all the State instances using OldContract.
      partyAProxy.vaultQuery(OldState::class.java).states
          .filter { stateAndRef ->
            stateAndRef.state.contract == OldContract.id
          }
          .map { stateAndRef ->
            partyAProxy.startFlowDynamic(
                ContractUpgradeFlow.Initiate::class.java,
                stateAndRef,
                NewContractWithLegacyConstraint::class.java)
          }.map {
            it.returnValue.getOrThrow()
          }

      // Log all the State instances in the vault to show they are using NewContract.
      val newPartyAStates = partyAProxy.vaultQuery(com.upgrade.legacy.constraint.new.NewState::class.java).states
      assertEquals("there should be one new state on party a", 1, newPartyAStates.size)
    }
  }
}