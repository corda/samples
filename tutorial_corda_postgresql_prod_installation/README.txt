this follows: https://docs.corda.r3.com/node-database-admin.html

0. Prerequisites

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

1. Creating a database user with schema permissions /PostgreSQL

show content and run a database administrator: 
party_a.sql
party_b.sql

you may log to db as admin and show that schema party_a was created but is empty.

2. Database schema creation

This will be done form db_admin, this simulates and db administrator who runs form different machine and has no access to nodes.

2.1. Create Liquibase management tables/ PostgreSQL

run as database admin:
control_table_party_a.sql
control_table_party_b.sql

2.2. Configure the Database Management Tool / PostresSQL

this is preconfigured: show db_admin/PartyA/node.conf 
it contains only database settings to connect as a database use without admin permissions - so admin knows
nothing will be altered.

db_admin/PartyA/drivers - contained JDBC driver
db_admin/PartyA/cordapps - contains Cordapp, it's important as DDL for cordapps is created in this step as well

2.3. Extract the DDL script using Database Management Tool

   java -jar tools-database-manager-4.1.jar dry-run -b db_admin/PartyA
   java -jar tools-database-manager-4.1.jar dry-run -b db_admin/PartyB


2.4. Apply DDL scripts on a database

connect using your tool as database administrator, 
//psql commandline could be used as well

3. Corda node configuration/PostgreSQL (this is similar to step 3 from simplified setup)


//The web page containing the example config, in our case we use localhost and default Postgres port 5432.
//Also each node has different database user name and schema.
// Because we create users and schema with different names that ones on docs, we also has 
// node configuration specific to each node:
cat party_a_database.conf
cat party_b_database.conf
//explain each config - the runMigration=true however this will take no effect, as the node will have no database  
// permissions, and all tables were created in point 2.4, however it's more informative to change to runMigration=false
// so by reading the config we konw the intention of the node is not to auto-create tables
//As written at the bottom, we will copy JDBC driver directly to node's direcotries, beacuse of that
//we don not provide path to JDBC driver in configuration fiel. If we have JDBC driver somewhere outised node's folde
//that we can use jarDirs property wich contain a absolut path to a folder with JDBC driver.

//Lets append to node configs
cat party_a_database.conf >> nodes/PartyA/node.conf
cat party_b_database.conf >> nodes/PartyB/node.conf

4. Node configuration - adding JDBC driver (this is a copy without first link of step 4 from simplified setup)

//You need to dowlnoad JDBC driver (e.g. curl https://jdbc.postgresql.org/download/postgresql-42.1.4.jar --output postgresql-42.1.4.jar ) but we already have a JAR, let's copy to each nodes:

cp postgresql-42.1.4.jar nodes/PartyA/drivers/
cp postgresql-42.1.4.jar nodes/PartyB/drivers/
