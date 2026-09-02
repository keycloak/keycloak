#!/usr/bin/env bash
set -e

./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -pl .
