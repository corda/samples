package bootcamp;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import java.util.ArrayList;
import java.util.List;
@InitiatingFlow
@StartableByRPC
public class GetAllTokensFlow extends FlowLogic {
    @Override
    @Suspendable
    public Object call() throws FlowException {
        return getServiceHub().getVaultService().queryBy(TokenState.class).getStates();
    }
}