package com.template;

import io.bluebank.braid.corda.BraidConfig;
import io.vertx.core.http.HttpServerOptions;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

/**
 * A regular Corda service that bootstraps the Braid server when the node
 * starts.
 *
 * The Braid server offers a user-defined set of flows and services.
 *
 * @property serviceHub the node's `AppServiceHub`.
 */
@CordaService
public class BootstrapBraidService extends SingletonSerializeAsToken {
    private final AppServiceHub serviceHub;

    public BootstrapBraidService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        (new BraidConfig())
                // Include a flow on the Braid server.
                .withFlow(WhoAmIFlow.class)
                // Include a service on the Braid server.
                .withService("myService", new BraidService(serviceHub))
                // The port the Braid server listens on.
                .withPort(8080)
                // Using http instead of https.
                .withHttpServerOptions(new HttpServerOptions().setSsl(false))
                // Start the Braid server.
                .bootstrapBraid(serviceHub, null);
    }
}
