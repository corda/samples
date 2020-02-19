package com.flowhttp

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import okhttp3.OkHttpClient
import okhttp3.Request

const val BITCOIN_README_URL = "https://raw.githubusercontent.com/bitcoin/bitcoin/4405b78d6059e536c36974088a8ed4d9f0f29898/readme.txt"

@InitiatingFlow
@StartableByRPC
class HttpCallFlow : FlowLogic<String>() {
    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {
        val httpRequest = Request.Builder().url(BITCOIN_README_URL).build()

        // BE CAREFUL when making HTTP calls in flows:
        // 1. The request must be executed in a BLOCKING way. Flows don't
        //    currently support suspending to await an HTTP call's response
        // 2. The request must be idempotent. If the flow fails and has to
        //    restart from a checkpoint, the request will also be replayed
        val httpResponse = OkHttpClient().newCall(httpRequest).execute()

        return httpResponse.body().string()
    }
}
