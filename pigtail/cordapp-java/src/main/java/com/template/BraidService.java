package com.template;


import net.corda.core.node.ServiceHub;

/**
 * Braid services are classes that define functions that Braid will expose
 * via the Braid server running on the node.
 *
 * Our service defines a single method, `whoAmiI`, that returns our node's
 * name.
 *
 * Braid services do not have to follow a specific format:
 *   * They do not have to implement a specific interface or subclass a
 *     specific class
 *   * They can take as many constructor arguments as you like
 *
 * @property serviceHub the node's `ServiceHub`.
 */
public class BraidService {
    private ServiceHub serviceHub;

    public BraidService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    public String whoAmI() {
        return serviceHub.getMyInfo().getLegalIdentities().get(0).getName().getOrganisation();
    }
}
