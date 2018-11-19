package net.corda.examples.oracle.service.service

import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.examples.oracle.base.contract.PrimeContract
import java.math.BigInteger

// We sub-class 'SingletonSerializeAsToken' to ensure that instances of this class are never serialised by Kryo.
// When a flow is check-pointed, the annotated @Suspendable methods and any object referenced from within those
// annotated methods are serialised onto the stack. Kryo, the reflection based serialisation framework we use, crawls
// the object graph and serialises anything it encounters, producing a graph of serialised objects.
// This can cause issues. For example, we do not want to serialise large objects on to the stack or objects which may
// reference databases or other external services (which cannot be serialised!). Therefore we mark certain objects with
// tokens. When Kryo encounters one of these tokens, it doesn't serialise the object. Instead, it creates a
// reference to the type of the object. When flows are de-serialised, the token is used to connect up the object
// reference to an instance which should already exist on the stack.
@CordaService
class Oracle(val services: ServiceHub) : SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey

    // Generates a list of natural numbers and filters out the non-primes.
    // The reason why prime numbers were chosen is because they are easy to reason about and reduce the mental load
    // for this tutorial application.
    // Clearly, most developers can generate a list of primes and all but the largest prime numbers can be verified
    // deterministically in reasonable time. As such, it would be possible to add a constraint in the
    // [PrimeContract.verify] function that checks the nth prime is indeed the specified number.
    private val primes = generateSequence(1) { it + 1 }.filter { BigInteger.valueOf(it.toLong()).isProbablePrime(16) }

    // Returns the Nth prime for N > 0.
    fun query(n: Int): Int {
        require(n > 0) { "n must be at least one." } // URL param is n not N.
        return primes.take(n).last()
    }

    // Signs over a transaction if the specified Nth prime for a particular N is correct.
    // This function takes a filtered transaction which is a partial Merkle tree. Any parts of the transaction which
    // the oracle doesn't need to see in order to verify the correctness of the nth prime have been removed. In this
    // case, all but the [PrimeContract.Create] commands have been removed. If the Nth prime is correct then the oracle
    // signs over the Merkle root (the hash) of the transaction.
    fun sign(ftx: FilteredTransaction): TransactionSignature {
        // Check the partial Merkle tree is valid.
        ftx.verify()

        /** Returns true if the component is an Create command that:
         *  - States the correct prime
         *  - Has the oracle listed as a signer
         */
        fun isCommandWithCorrectPrimeAndIAmSigner(elem: Any) = when {
            elem is Command<*> && elem.value is PrimeContract.Create -> {
                val cmdData = elem.value as PrimeContract.Create
                myKey in elem.signers && query(cmdData.n) == cmdData.nthPrime
            }
            else -> false
        }

        // Is it a Merkle tree we are willing to sign over?
        val isValidMerkleTree = ftx.checkWithFun(::isCommandWithCorrectPrimeAndIAmSigner)

        if (isValidMerkleTree) {
            return services.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}