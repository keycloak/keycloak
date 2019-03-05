#!/bin/bash -e

mvn help:evaluate -Dexpression=project.version -q -DforceStdout -pl .
