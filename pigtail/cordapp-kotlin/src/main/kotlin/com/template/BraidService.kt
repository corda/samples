package com.template

import net.corda.core.node.ServiceHub

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
class BraidService(val serviceHub: ServiceHub) {
    fun whoAmI() : String {
        return serviceHub.myInfo.legalIdentities.first().name.organisation
    }
}