package com.example.contract;

import com.example.state.SanctionableIOUState;
import com.example.state.SanctionedEntities;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [SanctionableIOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */

public class SanctionableIOUContract implements Contract {
    public static String IOU_CONTRACT_ID = "com.example.contract.SanctionableIOUContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);
            requireThat(require -> {
                require.using("All transactions require a list of sanctioned entities", tx.referenceInputRefsOfType(SanctionedEntities.class).size() >0);
                SanctionedEntities sanctionedEntities = tx.referenceInputRefsOfType(SanctionedEntities.class).get(0).getState().getData();
                require.using(sanctionedEntities.getIssuer().getName().getOrganisation() + " is an invalid issuer of sanctions lists for this contract", sanctionedEntities.getIssuer().getName().equals(((Commands.Create) (command.getValue())).getSanctionsBody().getName()));
                require.using("No inputs should be consumed when issuing an IOU.", tx.getInputs().isEmpty());
                require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
                SanctionableIOUState out = tx.outputsOfType(SanctionableIOUState.class).get(0);
                require.using("The lender and the borrower cannot be the same entity.", !(out.getLender().equals(out.getBorrower())));
                List<PublicKey> signerKeys = new ArrayList<>();
                for (AbstractParty party: out.getParticipants()
                     ) {
                    signerKeys.add(party.getOwningKey());

                }
                require.using("All of the participants must be signers.",command.getSigners().containsAll( signerKeys));

                // IOU-specific constraints.
                require.using("The IOU's value must be non-negative.", out.getValue() > 0);

                // IOU cannot involve a sanctioned entity
                require.using("The lender " + out.getLender().getName() + " is a sanctioned entity", !sanctionedEntities.getBadPeople().contains(out.getLender()));
                require.using("The borrower " + out.getBorrower().getName() + " is a sanctioned entity", !sanctionedEntities.getBadPeople().contains(out.getBorrower()));

                return null;
            });
        }
        /**
     * This contract only implements one command, Create.
     */
        public interface Commands extends CommandData {
            class Create implements Commands{
                private Party sanctionsBody;

                public Create(Party sanctionsBody) {
                    this.sanctionsBody = sanctionsBody;
                }

                public Party getSanctionsBody() {
                    return sanctionsBody;
                }
            };
        }
}


