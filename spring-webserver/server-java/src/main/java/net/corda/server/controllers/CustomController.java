package net.corda.server.controllers;

import net.corda.core.messaging.CordaRPCOps;
import net.corda.server.NodeRPCConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define CorDapp-specific endpoints in a controller such as this.
 */
@RestController
@RequestMapping("/custom") // The paths for GET and POST requests are relative to this base path.
public class CustomController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;

    public CustomController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
    }

    @GetMapping(value = "/customendpoint", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "Modify this";
    }
}
