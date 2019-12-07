<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Database Migration Tutorial

In this tutorial we will see how to do database migration from development and production perspective.
For this tutorial we will use docker postgresql. We will first connect to postgresql and then perform 
database migration.

Please note you will require Corda Enterprise Jar to run this tutorial.
Build this project using enterprsie jar.

## Development Mode

The node connects to the database in the development mode using admin access, 
gains full control over the database and automatically updates the database.

### Database SetUp

Follow below steps to connect to postgres docker.

#### Prerequisites
We will use IOU application from the sample directory. 
We will configure 3 nodes - Notary, PartyA, PartyB. 
Notary will connect to the default H2 database, hence no explicit database configuration is required. 
PartyA and PartyB will connect to PostgreSQL database, which will require configuring below mentioned steps.
You could use the latest PostgreSQL and latest jdbc PostgreSQL driver. 
Alteast PostgreSQL 9.6 must be used. 

#### Broadly you have to perform below steps to connect to PostgreSQL.
Creating a database user with administrative schema permissions.
Node Configuration (set runMigration = true).
Start the node.

##### Step 1 : Creating a database user with schema permissions and Start docker postgres container by running the below command
We are starting docker postgres container named postgres_for_corda.

    docker run --name postgres_for_corda -e POSTGRES_PASSWORD=my_password -d -p 5432:5432 postgres:11

Connect to the database using admin access.
execute script/dev/party_a.sql and script/dev/party_b.sql scripts to create users, schema's and assign permissions. 

    cat party_a.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres
    cat party_b.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres

##### Step 2 : Node Configuration
Deploy your IOU CorDapp and run below command to update each node's node.conf. This will append the database connection properties to node.conf.

    cat scripts/dev/party_a_database.conf >> nodes/PartyA/node.conf
    cat scripts/dev/party_b_database.conf >> nodes/PartyB/node.conf
    
Copy PostgreSQL jdbc driver to node's driver directory. You could also specify the drivers absolute path in jarDirs property in node.conf.

    cp postgresql-42.1.4.jar nodes/PartyA/drivers/
    cp postgresql-42.1.4.jar nodes/PartyB/drivers/
    
### Database Migration

Corda-the internal platform and CorDapps use Liquibase for database schema versioning.
This makes Corda database agnostic. CorDapp custom tables are created or upgraded automatically using Liquibase. 
Liquibase supports writing ddl/dml statements in many formats (XML, JSON, SQL, YAML). 
In development mode, we will connect the node to the database using admin access. 
When the node is started, the database agnostic scripts are automatically converted to database specific scripts and are applied automatically onto the database.

#### Broadly we will perform below steps to perform CorDapp database upgrade/migration.

##### Step 3 : Define Liquibase script

Lets first stop the nodes.
Create a migration folder in workflow-java/src/main/resources/migration.
Take a look a the liquibase script defined at workflows/v1-workflow/src/main/resources/migration/iou.changelog-v1.xml
Create a parent iou.changelog-master containing reference to iou.changelog-v1.xml.

##### Step 4:  Define custom schema

We have created an IOUState. We have also created IOUSchema, and overridden migrationResource field with iou.changelog-master name.
We have also provided the mapping from IOUState to the custom iou_states table in IOUState class.
Take a look at IOUState, IOUSchemaV1 classes.

##### Step 5 : Run the node

    cd build/nodes
    ./runnodes
    
Verify if iou_states table is created using

    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "\dt party_a_schema."

##### Step 6 : Stop the node. Update the schema by adding a new column. Create new Liquibase script and update master-changelog to include this.

We will make schema and state changes in contract v2. Now we will use contract v2 and workflow v2.

Take a look at the new IOUSchemaV1 and IOUState class in contract v2. We have added a new column named constraint_type.
Take a look at the new script defined at workflows/v2-workflow/src/main/resources/migration/iou.changelog-v2.xml
Add a reference to this script to iou-changelog-master.xml in workflows/v2-workflow/src/main/resources/migration/iou.changelog-master.xml

Run the gradle jar task on v2-workflows.

Replace the old jar in build/nodes/PartyA and build/nodes/PartyB with this new jar by running below script, or simply copy the jar from v2-workflows to 
build/nodes/PartyA and build/nodes/PartyB

    cd script
        ./upgrade.sh --node=PartyA,PartyB --workflow=2
        
##### Step 7: Start the node.

When the node starts liquibase will look at the databasechangelog table. 
As of now there is only an entry of iou-changelog-v1.xml in the table. 
Hence it knows that only iou-changelog-v1.xml has been executed. 
Hence it now picks up the new script which is iou-changelog-v2.xml, 
converts it to database scpecific scripts and executes it on the database.
An entry of iou-changelog-v2.xml is now added to the databasechangelog table.

Verify if the tables are created using

        docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "select * from party_a_schema.*"
________________________________________________________________________________________

## Production Mode

for production perspective its different, we don't want the node to perform schema creation automatically onto the database. 
Hence the node is always connected to the database using restrictive access. We also set the runMigration flag to false, to prevent the node 
from performing any automatic schema updates to the database. But if the node doesn't apply the scripts to the database, who will ? 
This is where the database management tool comes into picture, which is an essential tool used for production deployment. 
The database admin uses this tool to generate the database scripts by connecting the tool to the database using restrictive access. 
Once the scripts are generated, the database administrator will inspect them and apply them manually onto the database using his tool of choice.

### Database SetUp

This is similar to above dev setup. Except, we will be connecing the node to the db using restrictive access, and we will be setting 
the runMigration to false, so the node does not perform automatic db migration.

##### Step 1 : Creating a database user with schema permissions and Start docker postgres container by running the below command
We are starting docker postgres container named postgres_for_corda.

    docker run --name postgres_for_corda -e POSTGRES_PASSWORD=my_password -d -p 5432:5432 postgres:11

Connect to the database using admin access.
execute script/prod/party_a.sql and script/prod/party_b.sql scripts to create users, schema's and assign permissions. 

    cat party_a.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres
    cat party_b.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres

##### Step 2 : Node Configuration
Deploy your IOU CorDapp and run below command to update each node's node.conf. This will append the database connection properties to node.conf.

    cat scripts/prod/party_a_database.conf >> nodes/PartyA/node.conf
    cat scripts/prod/party_b_database.conf >> nodes/PartyB/node.conf
    
Copy PostgreSQL jdbc driver to node's driver directory. You could also specify the drivers absolute path in jarDirs property in node.conf.

    cp postgresql-42.1.4.jar nodes/PartyA/drivers/
    cp postgresql-42.1.4.jar nodes/PartyB/drivers/

### Database Migration

Follow similar instructions given in the dev mode.

Instead of running scripts from script/dev mode, in prod mode run the scripts from scripts/prod mode.
These scripts will set up user, schemas and assign restrictive access permissions, runMigration is also false.
In prod mode, the node will connect to the database using restrictive access. 
We will use the database migration tool to generate the migration scripts.

##### Database schema creation with Corda Database Management Tool

###### 1. Create Liquibase required tables

Liquibase internally uses databasechangelog table to maintain a list of changesets (each changeset is a list of DDL/DML queries) which have been executed onto the database till date.
When we do a fresh installation, database admin will have to maually run the below script to generate these tables. Create a file named databasechangelog.sql and copy below script to this file.

    CREATE TABLE "party_a_schema".databasechangelog (
    id varchar(255) NOT NULL,
    author varchar(255) NOT NULL,
    filename varchar(255) NOT NULL,
    dateexecuted timestamp NOT NULL,
    orderexecuted int4 NOT NULL,
    exectype varchar(10) NOT NULL,
    md5sum varchar(35) NULL,
    description varchar(255) NULL,
    comments varchar(255) NULL,
    tag varchar(255) NULL,
    liquibase varchar(20) NULL,
    contexts varchar(255) NULL,
    labels varchar(255) NULL,
    deployment_id varchar(10) NULL);
    
    CREATE TABLE "party_a_schema".databasechangeloglock (
    id int4 NOT NULL,
    locked bool NOT NULL,
    lockgranted timestamp NULL,
    lockedby varchar(255) NULL,
    CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id));
    
Execute the above script by running the below command

    cat databasechangelog.sql | docker exec -i postgres_for_corda psql -h localhost -p 5432 -U postgres
    
###### 2. Configure the Database Management Tool    

1. Create a new folder db_admin and create individual folders for all the nodes. Add node.conf, PostgreSQL jdbc driver and CorDapp jar to each node.
2. db_admin/PartyA/node.conf
    
        dataSourceProperties {
            dataSource {
                password="my_password"
                url="jdbc:postgresql://localhost:5432/postgres"
                user="party_a"
            }
            dataSourceClassName="org.postgresql.ds.PGSimpleDataSource"
        }
        database {
            transactionIsolationLevel=READ_COMMITTED
            schema="party_a_schema"
            runMigration=false
        }
        # When JDBC driver is not placed into node's 'drivers' directory then add absolute path:
        # jarDirs = ['absolute_path_to_directory_with_jdbc_driver']
        
        myLegalName="O=PartyA,L=Delhi,C=IN"
    
3. db_admin/PartyA/cordapps- add CorDapp.jar to this directory
4. db_admin/PartyA/drivers- add jdbc PostgreSQL driver to this directory.
5. database-management-tool.jar version should be same as the Corda version which you are using.

###### 3.  Extract the DDL script using Database Management Tool

Run the dry-run command to generate PostgreSQL specific scripts.

    java -jar tools-database-manager-4.1.jar dry-run -b db_admin/PartyA
    
###### 4.  Apply DDL scripts on a database

1. The database administrator is expected to manually check the above generated script, connect to the database and run the script manually using his tool of choice.
2. This is the only step when the database administrator connects to the database using admin access.
________________________


### Some useful docker commands to use
#####LIST ALL ROLES
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "\dg"

#####LIST ALL SCHEMAS
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "\dn"

#####ALL TABLES IN A SCHEMA
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "\dt party_a_schema.*"

#####SELECT CUSTOM TABLE ROWS
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "select * from party_a_schema.iou_states"

#####DROP SCHEMA
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "drop schema party_a_schema cascade"

#####DROP USER
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "REASSIGN OWNED BY party_a TO my_user"
    docker exec -i postgres_for_corda psql -U postgres -p 5432 -h localhost postgres -c "DROP role party_a"