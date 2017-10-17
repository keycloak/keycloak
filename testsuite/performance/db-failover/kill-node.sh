#!/bin/bash

. ./common.sh

if [ -z $1 ]; then echo "Specify DB node to kill."; exit 1; fi

killNode $1