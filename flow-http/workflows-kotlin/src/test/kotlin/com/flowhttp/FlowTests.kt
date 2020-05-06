package com.flowhttp

import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

const val BITCOIN_README_URL = "https://raw.githubusercontent.com/bitcoin/bitcoin/4405b78d6059e536c36974088a8ed4d9f0f29898/readme.txt"

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.flowhttp"))))
        a = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `test flow returns correct html`() {
        // The flow should return the first commit of the BitCoin readme.
        val flow = HttpCallFlow()
        val future = a.startFlow(flow)
        network.runNetwork()
        val returnValue = future.get()

        // We run the flow and retrieve its return value.
        val httpRequest = Request.Builder().url(BITCOIN_README_URL).build()
        val httpResponse = OkHttpClient().newCall(httpRequest).execute()
        val expectedValue = httpResponse.body()?.string()

        // We check that the strings are equal.
        assertEquals(expectedValue, returnValue)
    }
}