package net.corda.samples.states;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.samples.contracts.AssetContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * An ownable state to represent an asset that could be put on auction.
 */
@BelongsToContract(AssetContract.class)
public class Asset implements OwnableState, LinearState {

    private final UniqueIdentifier linearId;
    private final String title;
    private final String description;
    private final String imageUrl;

    private final AbstractParty owner;

    public Asset(UniqueIdentifier linearId, String title, String description, String imageUrl, AbstractParty owner) {
        this.linearId = linearId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.owner = owner;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }

    @NotNull
    @Override
    public AbstractParty getOwner() {
        return owner;
    }

    /**
     * This method should be called to retrieve an ownership transfer command and the updated state with the new owner
     * passed as a parameter to the method.
     *
     * @param newOwner of the asset
     * @return A CommandAndState object encapsulating the command and the new state with the changed owner, to be used
     * in the ownership transfer transaction.
     */
    @NotNull
    @Override
    public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
        return new CommandAndState(new AssetContract.Commands.TransferAsset(),
                new Asset(this.getLinearId(), this.getTitle(), this.getDescription(), this.getImageUrl(), newOwner ));
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
