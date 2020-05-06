package net.corda.examples.oracle.base.contract;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class PrimeContract implements Contract {
    public final static String PRIME_PROGRAM_ID = "net.corda.examples.oracle.base.contract.PrimeContract";

    // Our contract does not check that the Nth prime is correct. Instead, it checks that the
    // information in the command and state match.
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        Commands.Create command = requireSingleCommand(tx.getCommands(), Commands.Create.class).getValue();
        requireThat(req -> {
           req.using("There are must be no inputs", tx.getInputs().isEmpty());
           PrimeState output = tx.outputsOfType(PrimeState.class).get(0);
           req.using("The prime in the output does not match the prime in the command.",
                   (command.getN().equals(output.getN()) && command.getNthPrime().equals(output.getNthPrime())));
           return null;
        });
    }

    // Commands signed by oracles must contain the facts the oracle is attesting to.
    public interface Commands extends CommandData {
        class Create implements CommandData {
            private final Integer n;
            private final Integer nthPrime;

            public Create(Integer n, Integer nthPrime) {
                this.n = n;
                this.nthPrime = nthPrime;
            }

            public Integer getN() {
                return n;
            }

            public Integer getNthPrime() {
                return nthPrime;
            }
        }
    }
}
