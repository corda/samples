package net.corda.yo;

public class YoContractTests {

}


/*
* class YoContractTests {
    private val ledgerServices = MockServices(listOf("net.corda.yo", "net.corda.testing.contracts"))
    private val alice = TestIdentity(CordaX500Name("Alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("Bob", "Tokyo", "JP"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun yoTransactionMustBeWellFormed() {
        // A pre-made Yo to Bob.
        val yo = YoState(alice.party, bob.party)
        // Tests.
        ledgerServices.ledger {
            // Input state present.
            transaction {
                input(DummyContract.PROGRAM_ID, DummyState())
                command(alice.publicKey, YoContract.Send())
                output(YoContract.ID, yo)
                this.failsWith("There can be no inputs when Yo'ing other parties.")
            }
            // Wrong command.
            transaction {
                output(YoContract.ID, yo)
                command(alice.publicKey, DummyCommandData)
                this.failsWith("")
            }
            // Command signed by wrong key.
            transaction {
                output(YoContract.ID, yo)
                command(miniCorp.publicKey, YoContract.Send())
                this.failsWith("The Yo! must be signed by the sender.")
            }
            // Sending to yourself is not allowed.
            transaction {
                output(YoContract.ID, YoState(alice.party, alice.party))
                command(alice.publicKey, YoContract.Send())
                this.failsWith("No sending Yo's to yourself!")
            }
            transaction {
                output(YoContract.ID, yo)
                command(alice.publicKey, YoContract.Send())
                this.verifies()
            }
        }
    }
}
*
* */