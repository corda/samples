package net.corda.option.client.plugin

import net.corda.option.client.api.OptionApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

/**
 * Installing this plugin causes the CorDapp to offer a REST API and serve static web content.
 * Registered under src/resources/META-INF/services/.
 */
class OptionPlugin : WebServerPluginRegistry {

    override val webApis = listOf(Function(::OptionApi))

    override val staticServeDirs = mapOf(
            // This will serve the optionWeb directory in resources to /optionWeb
            "option" to javaClass.classLoader.getResource("optionWeb").toExternalForm()
    )
}