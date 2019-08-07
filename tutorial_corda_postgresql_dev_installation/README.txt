this follows: https://docs.corda.r3.com/head/node-database-developer.html

1. Prerequisites

// We have 3 nodes - a notary which will use H2 database and two nodes which will 
// store data in PostgreSQL database, 
// the nodes are already with set up with the pre-bootstrapped network (e.g. they were created by deployNodes Gradle task):

ls nodes
ls nodes/PartyA

//lets confirm we are using Corda Enteprise 4.1:

java -jar nodes/PartyA/corda.jar --version
Version: 4.1
Revision: 6b6c060a759b46b083d5e9ad46ca840704037deb
Platform Version: 4
Vendor: Corda Enterprise Edition


// node config is out-of-box with h2 database, we will let's change the config to use PostgreSql shortly:
cat nodes/PartyA/node.conf

2. Setup - prepare the database user and the database permissions:

// As previously described, we will setup a node to be able to auto-create tables upon startup,
//this is described in a simplified database schema setup:
https://docs.corda.r3.com/node-database-intro.html
//Choose:
https://docs.corda.r3.com/node-database-developer.html
// This page contain general instruction and specific configuration and DDL for each supported database 
// let's choose "Database setup for a new installation".
//(should lead to https://docs.corda.r3.com/node-database-developer.html#database-schema-setup)
// We have 3 steps:
//
//    1. Creating a database user with schema permissions
//    2. Corda node configuration changes
//    3. Run the node to auto-create schema objects

// We need to create database user and permissions for each node:
https://docs.corda.r3.com/node-database-developer.html#db-dev-setup-create-user-postgresql-ref

//We will use the same database for both nodes, but different users and each user has own schema,
//I've modified the DDL for the page to use specific user and schema, lets see:
cat party_a.sql

// Lines 1/2 We create party_a user  nd a schema party_a_schema
// Line 3 allow user to access the schema and create objects in that schema
// Line 4/5/6/7 adding permssions for current tables in that schema and for the tables created in the future 
// 8) beacuse the user name and the schema namespace are different we make any SQL run by our user to be prefixed 
by a schema namespace (in general Corda add such prefix to most of the SQL queries but not all)

// the DDL for PartyB contains the same script with different user name and schema name

 //Let's add execute DDL on the database, we are connecting as database administrator:

cat party_a.sql | psql -h localhost -p 5432 -U postgres -a
cat party_b.sql | psql -h localhost -p 5432 -U postgres -a

(the best is to run this to avoid any copy paste etc, psql should be available somewhere after installing Postgres. You could also use pgAdmin)

//( If you are using Docker, we could use:
//  cat party_a.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres -a
//  cat party_b.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres -a
//)

//We can see in the database there is a new schema but no tables, table will be create by the node (check using pgadmin)

3. Node configuration
(now if you are back to doc page you will see point 2 Corda node configuration, scroll a bit down and you will get link to Postgres, click, should lead to 
https://docs.corda.r3.com/node-database-developer.html#db-dev-setup-configure-node-postgresql-ref
)
//The web page containing the example config, in our case we use localhost and default Postgres port 5432.
//Also each node has different database user name and schema.
// Because we create users and schema with different names that ones on docs, we also has 
// node configuration specific to each node:
cat party_a_database.conf
cat party_b_database.conf

//As written at the bottom, we will copy JDBC driver directly to node's directories, beacause of that
//we don not provide path to JDBC driver in configuration field. If we have JDBC driver somewhere outside node's folder
//that we can use jarDirs property which contain an absolute path to a folder with JDBC driver.

//Lets append to node configs
cat party_a_database.conf >> nodes/PartyA/node.conf
cat party_b_database.conf >> nodes/PartyB/node.conf

4. Node configuration - adding JDBC driver

//You need to dowlnoad JDBC driver (e.g. curl https://jdbc.postgresql.org/download/postgresql-42.1.4.jar --output postgresql-42.1.4.jar ) but we already have a JAR, let's copy to each nodes:

cp postgresql-42.1.4.jar nodes/PartyA/drivers/
cp postgresql-42.1.4.jar nodes/PartyB/drivers/


5. Run:

//Lets runnodes:

nodes/runnodes 

//See welcome page PartyA and B connecting to Postgres database.


See the message is designed for production system, because a node can aoto-create tables (because it has database admin permissions lets enable runMigration to true)

6. test

//lets run simple flow in PartyA:
flow start CashIssueFlow amount: $500, issuerBankPartyRef: 3, notary: Notary

//then we can see in vault_states there one row.


7. summary - to wrap up what we did ( as listed in https://docs.corda.r3.com/node-database-developer.html#database-schema-setup)
we performed 3 steps:

    1) Created a database user with schema permissions
    2) Added database configuration to corda node and added JDBC driver 
    3) Run the nodes, the nodes have auto-created schema objects (tables/indexes)


------------------------------


sed -i -e 's/runMigration=false/runMigration=true/g' nodes/PartyA/node.conf
sed -i -e 's/runMigration=false/runMigration=true/g' nodes/PartyB/node.conf


Lest start:

flow start CashIssueFlow amount: $500, issuerBankPartyRef: 3, notary: Notary

The node has recorded all DDL statements in a table called databasechangelog, Corda uses
Liquibase to manage version of database schema.

runnodes 2
