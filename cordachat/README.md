# CordaChat

This app demonstrates how to build a simple chat program on top of the Corda framework.

To run it locally, follow these instructions:
 
1. [Download the DemoBench tool](https://www.corda.net/discover/demobench.html) or build it yourself from 
   the main source repository. We will use this to run the nodes.
2. Run `gradle build` to compile the CordaChat JAR.
3. Start DemoBench, add the app JAR you can find in `build/libs/cordachat.jar` and then start a notary and a couple
   of nodes. I tend to call them Alice and Bob.
4. Now you have two nodes running the app, open this project in IntelliJ and use the two run configs to start the GUI
   for Alice and Bob. They can chat to each other in separate windows.
   
# How does it work?

CordaChat is about 200 lines of Kotlin code. It starts by defining a simple state which holds a message, along with who
it is from and who it's to. The contract logic just ensures messages are signed by the party it claims to be from.

Then there's a flow to send a chat, which just creates and signs a transaction holding a message.

Then finally, there's the JavaFX GUI, which just downloads the vault and network map contents, binds them to the UI and
handles events.

# Future extensions

* Chat rooms, perhaps once data distribution groups are added to the platform.
* Try setting up nodes on the testnet.
* Use business networks to establish sub-sets of a wider zone, to narrow down the parties in the sidebar.