#!/usr/bin/env bash

# Upgrade a set of nodes to a given CorDapp version

if [[ "$#" -lt 3 ]]; then
    echo "Provide a version and some nodes to upgrade."
    exit 1
fi

function runCommand() {
    echo "Running $1"
    $1 2>&1 | /dev/null
    if [[ $? -ne 0 ]]; then
        exit 1
    fi
}

basedir="$1"
shift
contract_or_workflow="$1"
shift
version="$1"
shift
nodes=("$@")

./gradlew rpc-client:stopNodes
runCommand "scripts/copyUpgradeJars.sh ${basedir} ${contract_or_workflow} ${version} ${nodes[@]}"

build/nodes/runnodes

./gradlew rpc-client:runAttachmentLoading