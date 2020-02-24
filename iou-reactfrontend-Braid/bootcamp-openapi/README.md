# bootcamp-openapi
<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

## Introduction
This Cordapp is a modified version of the bootcamp Cordapp. It equipped with stand alone Braid server, which provides direct RPC communication to the Corda node. It is an out-of-box alternative web server solution to the SpringBoot.  

## How to add Braid to your project
Step 1: Add Braid to your dependencies
```
//Braid
    braid 'io.bluebank.braid:braid-server:4.1.2-RC13'
```
Step 2: Add generic function to start Braid
```
def startBraid(partyName, rpcHostAndPort, port) {
    println("starting braid for $partyName with rpc $rpcHostAndPort and port $port ")
    def partyDirectory = "build/nodes/${partyName}"
    def cordappDirectory = "${partyDirectory}/cordapps"
    // the following two effectively forces authentication via the REST API by passing
    // empty strings for user and password. replace this with "user1" and "test" to
    // automatically auth with node. N.B. configuring a username and password for RPC
    // disables the security at the REST API and is NOT secure!
    def rpcuser = "user1"
    // empty string
    def rpcpassword = "test"
    // empty string
    def braidPath = braidServerPath()
    javaexec {
        systemProperty "file.encoding", "UTF-8"
        // need this for braid to work with Windows at present
        main = "-jar"
        args = [
                braidPath,
                rpcHostAndPort,
                rpcuser,
                rpcpassword,
                port,
                3, // openapi v3 only supported
                cordappDirectory
        ]
    }
}
```
Step 3: Add gradle task to start individual server
```
task startBraidPartyA {
    doLast {
        startBraid("PartyA", "localhost:10004", 9004)
    }
}
```
Now, You will be able to run `./gradlew startBraidPartyA` to start the Braid Server.
