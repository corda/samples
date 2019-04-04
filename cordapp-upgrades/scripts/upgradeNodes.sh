#!/usr/bin/env bash

# Copy new versions of workflow or contract jars to the relevant nodes.

if [[ "$#" -lt 3 ]]; then
    echo "Provide a version and some nodes to upgrade."
    exit 1
fi

basedir=$(pwd)
contract_or_workflow="$1"
shift
version="$1"
shift
nodes=("$@")

for node in ${nodes[@]};
do cp ${basedir}/${contract_or_workflow}/${version}/build/libs/obligation-${contract_or_workflow}.jar ${basedir}/build/nodes/${node}/cordapps/
done;