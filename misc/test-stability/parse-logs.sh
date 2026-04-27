#!/bin/bash -e

LOG_DIR="$1"

if [ "$LOG_DIR" == "" ]; then
  echo "usage: parse-logs.sh <log directory>"
  exit 1
fi

java -jar target/test-logs-parser.jar "$1" "$2"
