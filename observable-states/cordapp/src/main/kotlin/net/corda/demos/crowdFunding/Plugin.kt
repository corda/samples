package net.corda.demos.crowdFunding

import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.transactions.TransactionBuilder

class Plugin : SerializationWhitelist {
    override val whitelist: List<Class<*>>
        get() = listOf(
                TransactionBuilder::class.java
        )
}