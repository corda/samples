package net.corda.examples.stockexchange.flows.utilities;

import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import net.corda.core.node.ServiceHub;

/**
 * This is a temporary helping class for getting TokenSelection for Java
 *
 */
public class TempTokenSelectionFactory {

    public static TokenSelection getTokenSelection(ServiceHub serviceHub){
        /* The following parameters are default values
            - maxRetries : 8
            - retrySleep : 100
            - retryCap   : 200
          */
        return new TokenSelection(serviceHub, 8, 100, 200);
    }
}
