package net.corda.examples.stockexchange.flows.utilities;

import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import net.corda.core.node.ServiceHub;

/**
 * This is a temporary helping class for getting TokenSelection for Java
 */
public class TempTokenSelectionFactory {

    public static TokenSelection getTokenSelection(ServiceHub serviceHub){
        return new TokenSelection(serviceHub, 8, 100, 200);
    }
}
