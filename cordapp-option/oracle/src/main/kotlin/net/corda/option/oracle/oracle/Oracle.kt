package net.corda.option.oracle.oracle

import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.option.base.KNOWN_SPOTS
import net.corda.option.base.KNOWN_VOLATILITIES
import net.corda.option.base.SpotPrice
import net.corda.option.base.Volatility
import net.corda.option.base.contract.OptionContract
import java.time.Instant

/**
 *  We sub-class 'SingletonSerializeAsToken' to ensure that instances of this class are never serialised by Kryo. When
 *  a flow is check-pointed, the annotated @Suspendable methods and any object referenced from within those annotated
 *  methods are serialised onto the stack. Kryo, the reflection based serialisation framework we use, crawls the object
 *  graph and serialises anything it encounters, producing a graph of serialised objects.
 *
 *  This can cause issues. For example, we do not want to serialise large objects on to the stack or objects which may
 *  reference databases or other external services (which cannot be serialised!). Therefore we mark certain objects
 *  with tokens. When Kryo encounters one of these tokens, it doesn't serialise the object. Instead, it creates a
 *  reference to the type of the object. When flows are de-serialised, the token is used to connect up the object
 *  reference to an instance which should already exist on the stack.
 */
@CordaService
class Oracle(val services: ServiceHub) : SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey

    private val knownSpots = KNOWN_SPOTS
    private val knownVolatilities = KNOWN_VOLATILITIES

    /** Returns spot for a given stock. */
    fun querySpot(stock: String, atTime: Instant): SpotPrice {
        return knownSpots.find { it.stock == stock && it.atTime == atTime } ?: throw IllegalArgumentException("Unknown spot.")
    }

    /** Returns volatility for a given stock. */
    fun queryVolatility(stock: String, atTime: Instant): Volatility {
        return knownVolatilities.find { it.stock == stock && it.atTime == atTime } ?: throw IllegalArgumentException("Unknown volatility.")
    }

    /**
     * Signs over a transaction if the specified spot price and volatility are correct.
     * This function takes a filtered transaction which is a partial Merkle tree. Any parts of the transaction which
     * the oracle doesn't need to see in order to verify the correctness of the nth prime have been removed. In this
     * case, all but the [OptionContract.OracleCommand] commands have been removed. If the spot price and
     * volatility are correct then the oracle signs over the Merkle root (the hash) of the transaction.
     */
    fun sign(ftx: FilteredTransaction): TransactionSignature {
        // Is the partial Merkle tree valid?
        ftx.verify()

        /** Returns true if the component is an OracleCommand that:
         *  - States the correct price and volatility
         *  - Has the oracle listed as a signer
         */
        fun isCommandWithCorrectPriceAndVolatilityAndIAmSigner(elem: Any) = when {
            elem is Command<*> && elem.value is OptionContract.OracleCommand -> {
                val cmdData = elem.value as OptionContract.OracleCommand
                val cmdSpotPrice = cmdData.spotPrice
                val cmdVolatility = cmdData.volatility
                myKey in elem.signers
                        && querySpot(cmdSpotPrice.stock, cmdSpotPrice.atTime) == cmdData.spotPrice
                        && queryVolatility(cmdVolatility.stock, cmdVolatility.atTime) == cmdData.volatility
            }
            else -> false
        }

        // Is it a Merkle tree we are willing to sign over?
        val isValidMerkleTree = ftx.checkWithFun(::isCommandWithCorrectPriceAndVolatilityAndIAmSigner)

        return if (isValidMerkleTree) {
            services.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}