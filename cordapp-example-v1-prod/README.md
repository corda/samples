<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Example CorDapp Database Schema Migration - production perspective - part 1

In this tutorial we will take a look at how perform database schema migration. This project talks about database migration from development/testing
perspective.

## Step 1 - Connect to PostgreSQL

You can take a look at the below video which talks about how to connect to PostgreSQL

https://www.youtube.com/watch?v=0BKUUY4Tg20

1.1 Install and start postgres database

1.2 Download postgres jdbc driver postgresql-42.1.4.jar 

1.3 Create the Corda Database user and schema permissions

https://docs.corda.r3.com/node-database-developer.html#postgresql
It contains user called "my_user" with password "my_password" and all permissions to "my_schema" schema namespace. 
Run this scrip using any SQL tool connected to PostgreSQL.

1.4 cordapp-example-v1-prod/party_a.sql provides you the script to create the user, schema, assign restrictive permissions to the user for the schema.


## Step 2 - SetUp Project

2.1 Download this project

    git clone https://github.com/corda/samples corda-samples
    git checkout -b database-migration
    cd cordapp-example-v1-prod
   
2.2 Update build.gradle
Add database connection properties 
Add postgresql jdbc drivers path
Add the admin user to the conf

    extraConfig = ['dataSourceProperties.dataSourceClassName' : 'org.postgresql.ds.PGSimpleDataSource',
                            'dataSourceProperties.dataSource.url' : 'jdbc:postgresql://localhost:5432/postgres',
                            'dataSourceProperties.dataSource.password' : 'my_password',
                            'dataSourceProperties.dataSource.user' : 'party_a_normal',
                            'database.transactionIsolationLevel' : 'READ_COMMITTED',
                            'database.schema' : 'party_a_schema',
                            'database.runMigration' : 'true' ]
    drivers = ['/Users/Downloads/postgresql-42.1.4.jar']

2.3 Update your repositories.gradle to point to the path containing enterprise dependencies

    maven {
        url 'file:///Users/nimmaj/dev/r3/enterpriseM2/repository-RELEASE_V_PLACEHOLDER'
    }
    
2.4 Add database management scripts

    <?xml version="1.1" encoding="UTF-8" standalone="no"?>
    <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                       xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">
        <changeSet author="R3.Corda" id="create_iou_state">
            <createTable tableName="iou_states">
            <column name="output_index" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="transaction_id" type="NVARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="lender" type="NVARCHAR(64)"/>
            <column name="borrower" type="NVARCHAR(64)"/>
            <column name="value" type="int"/>
                <column name="linear_id" type="uuid"/>
                <column name="sender" type="NVARCHAR(255)">
            </column>
        </createTable>
        </changeSet>
    </databaseChangeLog>
    
## Step 3 - Run the database management tool
Run the dry-run command of db management tool to generate the scripts
Manually execute these scripts on the database.

## Step 4 - Run the nodes
3.1 Deploy the nodes

    ./graldew workflows-java:deployNodes 
    
3.2 Hit the runnodes

    workflows-java/build/nodes/runnodes
   
3.3 The node should start as all the required database schemas are created in the database.

3.4 Stop the node.

Next Step - Refer to project cordapp-example-v2-dev where we have added a new column, added the new script.

3.5 We will make some changes in the state - add new column , add new changeset, build the jar, replace this old jar in all the nodes
with new ones and start off the node.

