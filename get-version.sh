#!/bin/bash -e

./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -pl .
