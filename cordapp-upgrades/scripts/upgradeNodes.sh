#!/bin/bash

# Upgrade a set of nodes to a given CorDapp version

if [[ "$#" -lt 2 ]]; then
    echo "Provide a version and some nodes to upgrade."
    exit 1
fi

version="$1"
shift
nodes="$@"

./gradlew rpc-client:stopNodes
for node in "$nodes";
do cp ${version}/build/libs/obligation.jar build/nodes/${node}/cordapps/;
done;

build/nodes/runnodes

./gradlew rpc-client:runAttachmentLoading