#!/usr/bin/env bash

for i in "$@"
do
  case $i in
    -n=*|--node=*)
    NODES="${i#*=}"
    ;;
    -c=*|--contract*)
    CONTRACT="${i#*=}"
    ;;
    -w=*|--workflow*)
    FLOW="${i#*=}"
    ;;
  esac
done


NODE=(${NODES//,/ })
for i in "${!NODE[@]}"
do
    if [[ "${CONTRACT}" != "" ]]; then
        rm  ../build/nodes/${NODE[i]}/cordapps/*-contracts.jar
        cp ../contracts/v${CONTRACT}-contracts/build/libs/v${CONTRACT}-contracts.jar ../build/nodes/${NODE[i]}/cordapps/
    fi

    if [[ "${FLOW}" != "" ]]; then
        rm  ../build/nodes/${NODE[i]}/cordapps/*-workflows.jar
        cp ../workflows/v${FLOW}-workflows/build/libs/v${FLOW}-workflows.jar ../build/nodes/${NODE[i]}/cordapps/
    fi
done

