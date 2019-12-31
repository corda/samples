package net.corda.examples.stockpaydividend.flows.utilities;

import com.google.common.collect.ImmutableList;

import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ObserversUtilities {

    // Hard coded the notary names here.
    public static List<String> getObserversNames(){
        return ImmutableList.of("Observer");
    }

    public static List<Party> getObserverLegalIdenties(IdentityService identityService){
        List<Party> observers = new ArrayList<>();
        for(String observerName : getObserversNames()){
            Set<Party> observerSet = identityService.partiesFromName(observerName, false);
            if (observerSet.size() != 1) {
                final String errMsg = String.format("Found %d identities for the observer.", observerSet.size());
                throw new IllegalStateException(errMsg);
            }
            observers.add(observerSet.iterator().next());
        }
        return observers;
    }

}
