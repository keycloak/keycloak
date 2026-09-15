#!/usr/bin/env bash
set -e

mvn -q clean install
java -jar target/migration-util.jar $1

