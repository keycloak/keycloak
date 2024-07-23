#!/bin/bash
rm -rf dist/*
cat theme/keywind/login/resources/dist/index.css theme/vunet/login/resources/custom.css | sed -e "s|/\*\(\\\\\)\?\*/|/~\1~/|g" -e "s|/\*[^*]*\*\+\([^/][^*]*\*\+\)*/||g" -e "s|\([^:/]\)//.*$|\1|" -e "s|^//.*$||" | tr '\n' ' ' | sed -e "s|/\*[^*]*\*\+\([^/][^*]*\*\+\)*/||g" -e "s|/\~\(\\\\\)\?\~/|/*\1*/|g" -e "s|\s\+| |g" -e "s| \([{;:,]\)|\1|g" -e "s|\([{;:,]\) |\1|g" > theme/keywind/login/resources/index_gen.css
pushd scripts/policy || exit
zip -r vsmaps-extensions.jar META-INF/ evaluate-privileges.js group-id-mapper.js
popd || exit
mkdir -p dist
mv scripts/policy/vsmaps-extensions.jar dist/vsmaps-extensions.jar
