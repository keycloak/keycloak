#!/bin/bash

cd $(readlink -f `dirname $0`)

python gitlab-conversion.py
cd target
asciidoctor master.adoc
