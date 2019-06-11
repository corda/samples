package com.example.server;

import com.example.flow.ExampleFlow;
import com.example.state.IOUState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
@RequestMapping("/api/example")
public class JavaController {

    private static final Logger logger = LoggerFactory.getLogger(JavaController.class);

    private final NodeRPCConnection nodeRPCConnection;

    private final CordaX500Name myLegalName;
    private final CordaRPCOps proxy;

    public JavaController(NodeRPCConnection nodeRPCConnection) {
        this.nodeRPCConnection = nodeRPCConnection;
        this.proxy = this.nodeRPCConnection.getProxy();
        this.myLegalName = this.proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @GetMapping(value = {"me"}, produces = {APPLICATION_JSON_VALUE})
    Map<String, CordaX500Name> whoami() {
        return Collections.singletonMap("me", myLegalName);
    }

    @PostMapping(value = "create-iou", produces = TEXT_PLAIN_VALUE, headers = "Content-Type=application/x-www-form-urlencoded")
    ResponseEntity<String> createIOU(HttpServletRequest request) {
        int iouValue = Integer.parseInt(request.getParameter("iouValue"));
        String partyName = request.getParameter("partyName");
        if (Strings.isEmpty(partyName)) {
            return ResponseEntity.badRequest().body("Query parameter 'partyName' not present.\n");
        }
        if (iouValue < 1) {
            return ResponseEntity.badRequest().body("Query parameter 'iouValue' must be non-negative.\n");
        }
        CordaX500Name partyX500Name = CordaX500Name.parse(partyName);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);
        if (otherParty == null) {
            return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n");
        }

        try {
            SignedTransaction signedTransaction = proxy.startTrackedFlowDynamic(ExampleFlow.Initiator.class, iouValue, otherParty).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTransaction.getId() + " committed to ledger.\n");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            ResponseEntity.badRequest().body(e.getMessage());
        }
        return null;
    }

    ResponseEntity<List<StateAndRef<IOUState>>> getIOUs() {
        return ResponseEntity.ok(proxy.vaultQuery(IOUState.class).getStates());
    }
}
