package net.corda.chat

import co.paralleluniverse.fibers.Suspendable
import com.github.ricksbrown.cowsay.Cowsay
import javafx.application.Application
import javafx.beans.Observable
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import net.corda.client.jfx.utils.observeOnFXThread
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.node.services.Vault
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.ProgressTracker
import org.apache.activemq.artemis.api.core.ActiveMQNotConnectedException
import java.security.PublicKey
import java.time.Instant
import java.time.ZoneId
import kotlin.system.exitProcess

class Chat : Contract {
    @CordaSerializable
    data class Message(val message: String, val to: AbstractParty, val from: AbstractParty,
                       override val participants: List<AbstractParty> = listOf(to, from)) : ContractState

    object SendChatCommand : TypeOnlyCommandData()

    override fun verify(tx: LedgerTransaction) {
        val signers: List<PublicKey> = tx.commandsOfType<SendChatCommand>().single().signers
        val message: Message = tx.outputsOfType<Message>().single()
        requireThat {
            "the chat message is signed by the claimed sender" using (message.from.owningKey in signers)
        }
    }
}

@InitiatingFlow
@StartableByRPC
class SendChat(private val to: Party, private val message: String) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker(object : ProgressTracker.Step("Sending") {})

    @Suspendable
    override fun call() {
        val stx: SignedTransaction = createMessageStx()
        progressTracker.nextStep()
        subFlow(FinalityFlow(stx))
    }

    private fun createMessageStx(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txb = TransactionBuilder(notary)
        val me = ourIdentityAndCert.party
        txb.addOutputState(Chat.Message(message, to, me), Chat::class.qualifiedName!!)
        txb.addCommand(Chat.SendChatCommand, me.owningKey)
        return serviceHub.signInitialTransaction(txb)
    }
}

class ChatApp : Application() {
    private var rpc: CordaRPCOps? = null

    private val host: String
        get() = if (parameters.unnamed.size > 0) parameters.unnamed[0] else "localhost:10006"

    // Inside Graviton, this runs whilst the spinner animation is active, so we can be kinda slow here.
    override fun init() {
        try {
            rpc = CordaRPCClient(NetworkHostAndPort.parse(host)).start("guest", "letmein").proxy
        } catch (e: ActiveMQNotConnectedException) {
            // Delay showing the error message until we're inside start(), when JavaFX is started up fully.
        }
    }

    override fun start(stage: Stage) {
        val fxml = FXMLLoader(ChatApp::class.java.getResource("chat.fxml"))
        stage.scene = Scene(fxml.load())
        stage.scene.stylesheets.add("/net/corda/chat/chat.css")
        val controller: ChatUIController = fxml.getController()
        val rpc = rpc   // Allow smart cast
        if (rpc == null) {
            Alert(Alert.AlertType.ERROR, "Could not connect to server: $host").showAndWait()
            exitProcess(1)
        }
        controller.rpc = rpc
        val me: Party = rpc.nodeInfo().legalIdentities.single()
        controller.linkPartyList(me)
        controller.linkMessages()
        stage.title = me.name.organisation
        if ("Graviton" !in this.javaClass.classLoader.toString()) {
            stage.width = 940.0
            stage.height = 580.0
        }
        stage.show()
    }
}

@Suppress("UNUSED_PARAMETER")
class ChatUIController {
    class ClickableParty(val party: Party) {
        val isNotary: Boolean get() = "Notary" in party.name.organisation
        override fun toString() = party.name.organisation
    }

    lateinit var textArea: TextArea
    lateinit var messageEdit: TextField
    lateinit var identitiesList: ListView<ClickableParty>
    lateinit var rpc: CordaRPCOps
    lateinit var usernameLabel: Label

    fun sendMessage(event: ActionEvent) {
        send(messageEdit.text)
    }

    private fun send(message: String) {
        messageEdit.text = "Sending ..."
        messageEdit.isDisable = true
        try {
            rpc.startFlow(::SendChat, identitiesList.selectionModel.selectedItem.party, message)
        } finally {
            messageEdit.isDisable = false
            messageEdit.text = ""
        }
    }

    fun onMoo(event: ActionEvent) {
        val m = if (messageEdit.text.isBlank()) "moo" else messageEdit.text
        send(":cow:$m")
    }

    fun linkPartyList(me: Party) {
        usernameLabel.text = "@" + me.name.organisation.toLowerCase()
        val (current, updates) = rpc.networkMapFeed()
        val names: List<ClickableParty> = current
                .flatMap { it.legalIdentities }
                .filterNot { it == me }
                .map { ClickableParty(it) }
                .filterNot { it.isNotary }
        identitiesList.items.addAll(names)
        identitiesList.selectionModel.selectedItems.addListener { _: Observable ->
            messageEdit.promptText = "Type message to ${identitiesList.selectionModel.selectedItems.first().party.name.organisation} here"
        }
        identitiesList.selectionModel.select(0)
        updates.observeOnFXThread().subscribe {
            if (it is NetworkMapCache.MapChange.Added)
                identitiesList.items.addAll(it.node.legalIdentities.map(::ClickableParty))
        }
    }

    fun linkMessages() {
        val messages = rpc.vaultTrack(Chat.Message::class.java)
        displayOldMessages(messages)
        messages.updates.observeOnFXThread().subscribe { it: Vault.Update<Chat.Message> ->
            if (it.containsType<Chat.Message>()) {
                val stateAndRef = it.produced.single()
                textArea.text += "[${Instant.now().formatted}] ${stateAndRef.from}: ${stateAndRef.msg}\n"
            }
        }
    }

    private fun displayOldMessages(messages: DataFeed<Vault.Page<Chat.Message>, Vault.Update<Chat.Message>>) {
        // Get a list of timestamps and messages.
        val timestamps = messages.snapshot.statesMetadata.map { it.recordedTime }
        val fromAndMessages = messages.snapshot.states.map {
            Pair(it.from, it.msg)
        }

        // Line them up and then sort them by time.
        val contents = (timestamps zip fromAndMessages).sortedBy { it.first }

        val sb = StringBuilder()
        for ((time, fromAndMessage) in contents) {
            sb.appendln("[${time.formatted}] ${fromAndMessage.first}: ${fromAndMessage.second}")
        }
        textArea.text = sb.toString()
    }

    private val Instant.formatted: String get() = this.atZone(ZoneId.systemDefault()).let { "${it.hour}:${it.minute}" }
    private val StateAndRef<Chat.Message>.from get() = state.data.from.nameOrNull()!!.organisation
    private val StateAndRef<Chat.Message>.msg get() = if (state.data.message.startsWith(":cow:")) System.lineSeparator() + Cowsay.say(arrayOf(state.data.message.drop(5))) else state.data.message
}

fun main(args: Array<String>) {
    Application.launch(ChatApp::class.java, *args)
}