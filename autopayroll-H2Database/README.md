# Tutorial -- How to look into the Corda Vault.
<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

## Introduction 
This note is a tutorial of how to look into the default H2 DataBase (the Corda Vault). It uses [autopayroll-CordaService] cordapp as the base cordapp, and we will not dive into any details about the cordapp itself.

## Tools that are needed  
* H2 Database Engine: Link to download at [Here](https://www.h2database.com/html/download.html)


## Step 1: Code changes in build.gradle 
By default, the node will not expose the H2 database. To configure the node to expose its internal database over a socket, you need to specify the full netowrk address using the `h2settings`. More information can be found [here](https://docs.corda.net/node-database-access-h2.html)
However, since we are in a bootstrapped environment, we can simply make the changes to `build.gradle`. 

Add the following line to each node inside the `task deployNodes` with a different port address:
```
extraConfig = ['h2Settings.address' : 'localhost:XXXXX']
```
For example: 
<p align="center">
  <img src="https://github.com/corda/samples/blob/add-samples/autopayroll-H2Database/screenshots/extraConfig.png" alt="Corda" width="700" height="345">
</p>

Rebuild and run the sample by typing:
```
./gradlew clean deployNodes
./build/nodes/runnodes
```

By now, you should see the JDBC address in the node shell
For example: 
<p align="center">
  <img src="https://github.com/corda/samples/blob/add-samples/autopayroll-H2Database/screenshots/10035.png" alt="Corda" width="1000" height="300" >
</p>


## Step 2: Starting the H2 Database viewer and connect to Corda vault
Move to the `h2/bin`directory and start the H2 Database Engine by typing: 

Unix: `sh h2.sh`
Windows: `h2.bat`

We will see the admin console of H2 Database Engine:
<p align="center">
  <img src="https://github.com/corda/samples/blob/add-samples/autopayroll-H2Database/screenshots/H2Console.png" alt="Corda">
</p>
At the JDBC URL field, enter the the port address what is shown on the node shell. For this example: it is `jdbc:h2:tcp://localhost:10035/node`: 

<p align="center">
  <img src="https://github.com/corda/samples/blob/add-samples/autopayroll-H2Database/screenshots/10035jdbc.png" alt="Corda">
</p>

After hitting connect, we are in the Corda vault for the specfic node we choose. 



<p align="center">
  <img src="https://github.com/corda/samples/blob/add-samples/autopayroll-H2Database/screenshots/vault.png" alt="Corda">
</p>
