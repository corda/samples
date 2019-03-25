#!/usr/bin/env bash

# Upgrade a set of nodes to a given CorDapp version

if [[ "$#" -lt 2 ]]; then
    echo "Provide a version and some nodes to upgrade."
    exit 1
fi

basedir="$1"
shift
version="$1"
shift
nodes=("$@")

./gradlew rpc-client:stopNodes
for node in ${nodes[@]};
do cp ${basedir}/${version}/build/libs/obligation.jar ${basedir}/build/nodes/${node}/cordapps/;
done;

build/nodes/runnodes

./gradlew rpc-client:runAttachmentLoading