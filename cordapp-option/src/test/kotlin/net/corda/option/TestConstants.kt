package net.corda.option

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity

val DUMMY_LINEAR_ID = UniqueIdentifier.fromString("3a3be8e0-996f-4a9a-a654-e9560df52f14")
val MEGA_CORP = TestIdentity(CordaX500Name("MegaCorp", "", "GB"))
val MINI_CORP = TestIdentity(CordaX500Name("MiniCorp", "", "GB"))
val ORACLE = TestIdentity(CordaX500Name("Oracle", "", "GB"))
val NOTARY = TestIdentity(CordaX500Name("Notary", "", "GB"))