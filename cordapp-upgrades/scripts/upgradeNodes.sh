#!/usr/bin/env bash

# Upgrade a set of nodes to a given CorDapp version

./gradlew rpc-client:stopNodes
for node in "PartyA" "PartyB" "PartyC";
do cp v2-finality-upgrade/build/libs/obligation.jar build/nodes/${node}/cordapps/;
done;

build/nodes/runnodes

./gradlew rpc-client:runAttachmentLoading