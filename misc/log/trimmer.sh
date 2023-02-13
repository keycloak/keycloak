#!/bin/bash -e

cd $(dirname $0)

if [ ! -f "LogTrimmer.class" ]; then
    javac LogTrimmer.java
fi

java LogTrimmer
