package com.example.contract;

import com.example.state.InvoiceState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [InvoiceState], which in turn encapsulates an [InvoiceState].
 *
 * For a new [InvoiceState] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [InvoiceState].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class InvoiceContract implements Contract {
    public static String ID = "com.example.contract.InvoiceContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);
        if(command.getValue() instanceof  Commands.Create){
            requireThat( require -> {
               require.using("No inputs should be consumed when issuing an Invoice.", tx.getInputs().isEmpty());
               require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
               InvoiceState out = tx.outputsOfType(InvoiceState.class).get(0);
               require.using("The lender and the borrower cannot be the same entity.", out.getContractor() != out.getCompany() );
               List<PublicKey> publicKeys = new ArrayList<>();
               for (AbstractParty participant: out.getParticipants()
               ) {
                   publicKeys.add(participant.getOwningKey());
               };
               require.using("All of the participants must be signers.", command.getSigners().containsAll(publicKeys));

               //Invoice specific constraints
                require.using("The Invoice's value must be non-negative.", out.getHoursWorked() > 0);
                require.using("The invoice should not yet be paid", !out.getPaid());
                return null;
            });
        }else if(command.getValue() instanceof Commands.Pay){
            requireThat(require -> {
               require.using("Exactly one invoice state should be consumed when paying an Invoice.", tx.inputsOfType(InvoiceState.class).size() == 1);
               require.using("Expect only one invoice state output from paying an invoice.", tx.outputsOfType(InvoiceState.class).size() == 1);
               require.using("Expect at least one output in addition to invoice.", tx.getOutputs().size() >= 2);

                //Invoice-specific requirements
                // TODO: Support partial payments
               InvoiceState out =  tx.outputsOfType(InvoiceState.class).get(0);
               require.using("The invoice should now be paid.", out.getPaid());
               return null;
            });
        }
    }
    public interface Commands extends CommandData {
        class Create implements Commands{
            private Party contractor;
            private Party company;
            private Double rate;

            public Party getContractor() {
                return contractor;
            }

            public Party getCompany() {
                return company;
            }

            public Double getRate() {
                return rate;
            }

            public Create(Party contractor, Party company, Double rate){
                this.contractor = contractor;
                this.company = company;
                this.rate = rate;
            }
        }
        class Pay implements Commands{

        }

    }
}
