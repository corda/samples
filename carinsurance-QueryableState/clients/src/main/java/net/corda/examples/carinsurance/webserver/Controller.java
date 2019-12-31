package net.corda.examples.carinsurance.webserver;

import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.examples.carinsurance.flows.ClaimInfo;
import net.corda.examples.carinsurance.flows.InsuranceClaimFlow;
import net.corda.examples.carinsurance.flows.InsuranceInfo;
import net.corda.examples.carinsurance.flows.IssueInsuranceFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    /*
    * API to trigger the Insurance Issuance flow.
    **/
    @PostMapping(value = "/vehicleInsurance/{insuree}")
    private String vehicleSale(@RequestBody InsuranceInfo insuranceInfo, @PathVariable String insuree) {

        // Get the Party object from the partyName.
        Set<Party> matchingParties = proxy.partiesFromName(insuree, false);

        // Trigger IssueInsuranceInitiator flow.
        proxy.startFlowDynamic(IssueInsuranceFlow.IssueInsuranceInitiator.class, insuranceInfo,
                matchingParties.iterator().next());
        return "Issue Insurance Completed";
    }

    /*
     * API to trigger the Insurance Claim flow. It accepts the claim containing details of the claim and the
     * policyNumber of the insurance in passed as path variable.
     **/
    @PostMapping(value = "/vehicleInsurance/claim/{policyNumber}")
    private String claim(@RequestBody ClaimInfo claimInfo, @PathVariable String policyNumber) {

        // Trigger InsuranceClaimInitiator flow.
        proxy.startFlowDynamic(InsuranceClaimFlow.InsuranceClaimInitiator.class, claimInfo, policyNumber);
        return "Insurance Claim Completed";
    }
}