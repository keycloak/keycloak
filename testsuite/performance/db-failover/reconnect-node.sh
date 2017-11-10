#!/bin/bash

. ./common.sh

if [ -z $1 ]; then echo "Specify DB node to reconnect to cluster."; exit 1; fi

reconnectNode $1