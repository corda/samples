package net.corda.cordaftp

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.loggerFor
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

@CordaSerializable
enum class PostSendAction {
    DELETE {
        override fun doAction(file: String) {
            val log = loggerFor<Configuration>()
            val path = Paths.get(file)
            log.info("$this - Removing $path")
            Files.delete(path)
        }
    };
    //TODO Pass in TxConfiguration or a similar class
    abstract fun doAction(file: String)
}

data class TxConfiguration(val searchDirectory: String,
                           val searchPattern: String,
                           val logDirectory: String,
                           val destinationParty: String,
                           val myReference: String,
                           val theirReference: String,
                           val postSendAction: PostSendAction? = null) // TODO - change strings to paths etc.

data class RxConfiguration(val myReference: String,
                           val destinationDirectory: String,
                           val logDirectory: String)

data class Configuration(val defaults: Map<String, String> = mapOf(),
                         val txMap: Map<String, TxConfiguration> = mapOf(),
                         val rxMap: Map<String, RxConfiguration> = mapOf())

interface ConfigurationReader {
    fun readConfiguration(input: InputStream): Configuration
}

class FileConfigurationReader : ConfigurationReader {
    override fun readConfiguration(input: InputStream): Configuration = jacksonObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS).readValue(input)
}

class FakeConfigurationReader : ConfigurationReader {
    override fun readConfiguration(input: InputStream): Configuration {
        val dc1 = TxConfiguration("/Users/richardgreen/example_send/blerg", ".*\\.txt", "/Users/richardgreen/example_send/log", "NodeA", "my_reference", "other_nodes_reference_1")
        val dc2 = TxConfiguration("/Users/richardgreen/example_send/blah", ".*\\.txt", "/Users/richardgreen/example_send/log", "NodeA", "my_reference", "other_nodes_reference_2")

        val inc1 = RxConfiguration("incoming_ref1", "/Users/richardgreen/incoming_1", "/Users/richardgreen/log")
        val inc2 = RxConfiguration("incoming_ref2", "/Users/richardgreen/incoming_2", "/Users/richardgreen/log")

        return Configuration(
                mutableMapOf(Pair("environment", "dev")),
                mutableMapOf(
                        Pair("my_config_name1", dc1),
                        Pair("my_name", dc2)),
                mutableMapOf(
                        Pair("incoming_1", inc1),
                        Pair("incoming_2", inc2)
                )
        )
    }
}
