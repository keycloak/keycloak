#!/bin/bash -e

mvn help:evaluate -Dexpression=project.version -DforceStdout --quiet --non-recursive -pl .
