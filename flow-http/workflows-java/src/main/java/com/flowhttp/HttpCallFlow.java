package com.flowhttp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@InitiatingFlow
@StartableByRPC
public class HttpCallFlow extends FlowLogic<String> {
    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        final Request httpRequest = new Request.Builder().url(Constants.BITCOIN_README_URL).build();

        // BE CAREFUL when making HTTP calls in flows:
        // 1. The request must be executed in a BLOCKING way. Flows don't
        //    currently support suspending to await an HTTP call's response
        // 2. The request must be idempotent. If the flow fails and has to
        //    restart from a checkpoint, the request will also be replayed
        String value = null;
        Response httpResponse = null;
        try {
            httpResponse = new OkHttpClient().newCall(httpRequest).execute();
            value = httpResponse.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }
}
