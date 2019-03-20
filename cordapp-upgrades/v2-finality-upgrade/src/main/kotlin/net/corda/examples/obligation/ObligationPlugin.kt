package net.corda.examples.obligation

import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class ObligationPlugin : WebServerPluginRegistry {
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::ObligationApi))
    override val staticServeDirs: Map<String, String> = mapOf(
            "obligation" to javaClass.classLoader.getResource("obligationWeb").toExternalForm()
    )
}