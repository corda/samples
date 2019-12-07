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