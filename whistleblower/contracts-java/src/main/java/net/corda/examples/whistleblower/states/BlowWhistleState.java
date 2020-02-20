package net.corda.examples.whistleblower.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.examples.whistleblower.contracts.BlowWhistleContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A state representing a whistle-blowing case.
 *
 * The identity of both the whistle-blower and the investigator is kept confidential through the
 * use of [AnonymousParty].
 *
 * @property badCompany the company the whistle is being blown on.
 * @property whistleBlower the [AnonymousParty] blowing the whistle.
 * @property investigator the [AnonymousParty] handling the investigation.
 */
@BelongsToContract(BlowWhistleContract.class)
public class BlowWhistleState implements LinearState {
    private final Party badCompany;
    private final AnonymousParty whistleBlower;
    private final AnonymousParty investigator;
    private final UniqueIdentifier linearId = new UniqueIdentifier();

    public BlowWhistleState(Party badCompany, AnonymousParty whistleBlower, AnonymousParty investigator) {
        this.badCompany = badCompany;
        this.whistleBlower = whistleBlower;
        this.investigator = investigator;
    }

    public Party getBadCompany() {
        return badCompany;
    }

    public AnonymousParty getWhistleBlower() {
        return whistleBlower;
    }

    public AnonymousParty getInvestigator() {
        return investigator;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(whistleBlower, investigator);
    }
}
