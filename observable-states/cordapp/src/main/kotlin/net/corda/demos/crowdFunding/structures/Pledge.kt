package net.corda.demos.crowdFunding.structures

import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.demos.crowdFunding.contracts.PledgeContract
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Lob
import javax.persistence.Table

@BelongsToContract(PledgeContract::class)
data class Pledge(
        val amount: Amount<Currency>,
        val pledger: AbstractParty,
        val manager: Party,
        val campaignReference: UniqueIdentifier,
        override val participants: List<AbstractParty> = listOf(pledger, manager),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {
    override fun supportedSchemas() = listOf(PledgeSchemaV1)
    override fun generateMappedObject(schema: MappedSchema) = PledgeSchemaV1.PledgeEntity(this)

    object PledgeSchemaV1 : MappedSchema(Pledge::class.java, 1, listOf(PledgeEntity::class.java)) {
        @Entity
        @Table(name = "pledges")
        class PledgeEntity(
                @Column
                var currency: String,

                @Column
                var amount: Long,

                @Column
                @Lob
                var pledger: ByteArray,

                @Column
                @Lob
                var manager: ByteArray,

                @Column
                var campaign_reference: String,

                @Column
                var linear_id: String
        ) : PersistentState() {
            constructor(pledge: Pledge): this(
                    pledge.amount.token.toString(),
                    pledge.amount.quantity,
                    pledge.pledger.owningKey.encoded,
                    pledge.manager.owningKey.encoded,
                    pledge.campaignReference.id.toString(),
                    pledge.linearId.id.toString()
            )

            constructor(): this("", 0, ByteArray(0), ByteArray(0), "", "")
        }
    }
}