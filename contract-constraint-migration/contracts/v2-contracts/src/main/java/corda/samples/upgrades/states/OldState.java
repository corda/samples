        package corda.samples.upgrades.states;

import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.OldContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.List;

        @BelongsToContract(OldContract.class)
        public class OldState implements ContractState {

            private Party issuer;
            private Party owner;
            private int amount;

            public OldState(Party issuer, Party owner, int amount) {
                this.issuer = issuer;
                this.owner = owner;
                this.amount = amount;
            }

            public Party getIssuer() {
                return issuer;
            }

            public Party getOwner() {
                return owner;
            }

            public int getAmount() {
                return amount;
            }

            @NotNull
            @Override
            public List<AbstractParty> getParticipants() {
                return ImmutableList.of(issuer, owner);
            }
        }
