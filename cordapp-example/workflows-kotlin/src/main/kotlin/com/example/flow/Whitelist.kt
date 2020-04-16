package com.example.flow

import com.example.data.IOUData
import net.corda.core.serialization.SerializationWhitelist

class Whitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>>
        get() = listOf(
                IOUData::class.java
        )
}
