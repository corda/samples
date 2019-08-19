<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Example CorDapp Database Schema Migration - production perspective - part 2

This tutorial is in continuation of cordapp-example-v1-prod.

## Step 1 - Add new column to IOUState,IOUSchemaV1

Make sure to add @Nullable for these columns to remain backward compatible

    private final Integer constraint_type;
    
    public IOUState(Integer value,
                        Party lender,
                        Party borrower,
                        UniqueIdentifier linearId,
                        @Nullable Integer constraint_type)
                        
    public Integer getConstraint_type() {
            return constraint_type;
        }
        
    @Override public PersistentState generateMappedObject(MappedSchema schema) {
            if (schema instanceof IOUSchemaV1) {
                return new IOUSchemaV1.PersistentIOU(
                        this.lender.getName().toString(),
                        this.borrower.getName().toString(),
                        this.value,
                        this.linearId.getId(),this.constraint_type);
            } else {
                throw new IllegalArgumentException("Unrecognised schema $schema");
            }
        }
           
## Step 2 - Create liquibase script for new column

    <?xml version="1.1" encoding="UTF-8" standalone="no"?>
    <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                       xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">
        <changeSet author="R3.Corda1" id="add_iou_state_column">
            <addColumn tableName="iou_states">
            <column name="constraint_type" type="INT" defaultValue="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
        </changeSet>
    </databaseChangeLog>
    

## Step 3 - Add this new script to master-changelog

    <?xml version="1.1" encoding="UTF-8" standalone="no"?>
    <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                       xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">
    
        <include file="migration/iou.changelog-v1.xml"/>
        <include file="migration/iou.changelog-v2.xml"/>
    </databaseChangeLog>

## Step 4 - Build jar 

build jar and replace the old jars in project cordapp-example-v1-dev with these new jars.

## Step 5 - Run the database management tool
Run the dry-run command of db management tool to generate the scripts
Manually execute these scripts on the database.


## Step 6 - Start the node

The node should automatically create this new column in the database
     