#!/usr/bin/env bash

current_dir="$(pwd)"

echo "Running in ${current_dir}"

function runCommand() {
    echo "Running $1"
    $1 2>&1 | /dev/null
    if [[ $? -ne 0 ]]; then
        exit 1
    fi
}

# Deploy the nodes, then start all nodes at same workflow version
runCommand "./gradlew deployNodes"
runCommand "scripts/copyUpgradeJars.sh ${current_dir} workflows v2-finality-intermediate PartyA PartyB"

# Run the nodes and issue some obligations
runCommand "build/nodes/runnodes"
runCommand "./gradlew rpc-client:issueBetweenNodes"

# Now upgrade some of the nodes to the latest contract version
runCommand "./gradlew stopNodes"
runCommand "scripts/copyUpgradeJars.sh ${current_dir} workflows v4-with-default PartyA PartyB"
runCommand "scripts/copyUpgradeJars.sh ${current_dir} contracts v2-can-default PartyA PartyB"
runCommand "build/nodes/runnodes"

# Issue some obligations between all nodes
runCommand "./gradlew rpc-client:issueBetweenNodes"

# Upgrade the final node and change the config to set the nullable field
runCommand "./gradlew stopNodes"
runCommand "scripts/copyUpgradeJars.sh ${current_dir} workflows v4-with-default PartyC"
runCommand "scripts/copyUpgradeJars.sh ${current_dir} contracts v2-can-default PartyC"
# Change config file here for all nodes
runCommand "build/nodes/runnodes"

# Issue a final set of obligations, then settle them all
runCommand "./gradlew rpc-client:issueBetweenNodes"
runCommand "./gradlew rpc-client:settleAllObligations"
