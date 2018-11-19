package net.corda.examples.attachments.plugin

import net.corda.examples.attachments.api.AgreementApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class AgreementPlugin : WebServerPluginRegistry {
    override val webApis = listOf(Function(::AgreementApi))

    override val staticServeDirs = mapOf("a" to javaClass.classLoader.getResource("blacklistWeb").toExternalForm())
}