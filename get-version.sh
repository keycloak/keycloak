#!/bin/bash -e

awk '/"version":/ { print $2 }' package.json | sed 's/"//g' | sed 's/,//'