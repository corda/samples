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

runCommand "./gradlew deployNodes"
runCommand "scripts/copyUpgradeJars.sh ${current_dir} workflows v2-finality-intermediate PartyA PartyB"

runCommand "build/nodes/runnodes"

runCommand "./gradlew rpc-client:issueBetweenNodes"

runCommand "scripts/upgradeNodes.sh ${current_dir} contracts v2-can-default PartyA PartyB PartyC"

runCommand "./gradlew rpc-client:settleAllObligations"
