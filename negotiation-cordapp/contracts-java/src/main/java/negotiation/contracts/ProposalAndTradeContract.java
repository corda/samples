package negotiation.contracts;

import com.google.common.collect.ImmutableSet;
import negotiation.states.ProposalState;
import negotiation.states.TradeState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ProposalAndTradeContract implements Contract {
    public static String ID = "negotiation.contracts.ProposalAndTradeContract";
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);

        if( command.getValue() instanceof  Commands.Propose) {
            requireThat(require -> {
                require.using("There are no inputs", tx.getInputs().isEmpty());
                require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
                require.using("The single output is of type ProposalState", tx.outputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one command", tx.getCommands().size() == 1);
                require.using("There is no timestamp", tx.getTimeWindow() == null);
                ProposalState output = tx.outputsOfType(ProposalState.class).get(0);
                require.using("The buyer and seller are the proposer and the proposee", ImmutableSet.of(output.getBuyer(), output.getSeller()).equals(ImmutableSet.of(output.getProposee(), output.getProposer())));
                require.using("The proposer is a required signer", command.getSigners().contains(output.getProposer().getOwningKey()));
                require.using("The proposee is a required signer", command.getSigners().contains(output.getProposee().getOwningKey()));
                return null;
            });
        }else if(command.getValue() instanceof Commands.Accept){
            requireThat(require -> {
                require.using("There is exactly one input", tx.getInputStates().size() == 1);
                require.using("The single input is of type ProposalState", tx.inputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one output", tx.getOutputs().size() == 1);
                require.using("The single output is of type TradeState", tx.outputsOfType(TradeState.class).size() == 1);
                require.using("There is exactly one command", tx.getCommands().size() == 1);
                require.using("There is no timestamp", tx.getTimeWindow() == null);

                ProposalState input = tx.inputsOfType(ProposalState.class).get(0);
                TradeState output = tx.outputsOfType(TradeState.class).get(0);

                require.using("The amount is unmodified in the output", output.getAmount() == input.getAmount());
                require.using("The buyer is unmodified in the output", input.getBuyer().equals(output.getBuyer()));
                require.using("The seller is unmodified in the output", input.getSeller().equals(output.getSeller()));

                require.using("The proposer is a required signer", command.getSigners().contains(input.getProposer().getOwningKey()));
                require.using("The proposee is a required signer", command.getSigners().contains(input.getProposee().getOwningKey()));
                return null;
            });
        }else if(command.getValue() instanceof Commands.Modify){
            requireThat(require ->{
                require.using("There is exactly one input", tx.getInputStates().size() == 1);
                require.using("The single input is of type ProposalState", tx.inputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one output", tx.getOutputs().size() == 1);
                require.using("The single output is of type ProposalState", tx.outputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one command", tx.getCommands().size() == 1);
                require.using("There is no timestamp", tx.getTimeWindow() == null);

                ProposalState input = tx.inputsOfType(ProposalState.class).get(0);
                ProposalState output = tx.outputsOfType(ProposalState.class).get(0);

                require.using("The amount is unmodified in the output", output.getAmount() != input.getAmount());
                require.using("The buyer is unmodified in the output", input.getBuyer().equals(output.getBuyer()));
                require.using("The seller is unmodified in the output", input.getSeller().equals(output.getSeller()));

                require.using("The proposer is a required signer", command.getSigners().contains(input.getProposer().getOwningKey()));
                require.using("The proposee is a required signer", command.getSigners().contains(input.getProposee().getOwningKey()));
                return null;

            });
        }else{
            throw new IllegalArgumentException("Command of incorrect type");
        }

    }


    public interface Commands extends CommandData {
        class Propose implements Commands{};
        class Accept implements Commands{};
        class Modify implements Commands{};
    }
}
