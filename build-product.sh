#!/bin/bash

GUIDE=$1

cd $GUIDE
python gitlab-conversion.py
cd target
asciidoctor master.adoc

