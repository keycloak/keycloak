#!/bin/bash -ex
rm -rf build/ccutil
mkdir -p build/ccutil
cp -r build/assembler/guides-server build/ccutil
cp -r ccutil/guides-server/* build/ccutil/guides-server

# sed -i 's|xref:{page-version}@guides-server::\([^.]*\)|https://ahus1.github.io/keycloak-antora/guides-server/latest/\1|g' guides-operator/guides-operator/20.0/guides-keycloak-operator.adoc

IMAGENAME='quay.io/ivanhorvath/ccutil:amazing'

if which podman &>/dev/null;
then
    CRT=$(which podman)
else
    CRT=$(which docker)
fi

if ! ${CRT} exec ccutil true &>/dev/null;
then
    ${CRT} rm -f ccutil
    ${CRT} run --privileged --ulimit host -d -v $(pwd):/docs:rw --name ccutil ${IMAGENAME}
fi

${CRT} exec -w /docs/build/ccutil/guides-server ccutil ccutil compile --format html --lang en-US
${CRT} exec -w /docs/build/ccutil/guides-server ccutil ccutil compile --format html-single --lang en-US

rm -rf _site/guides-server-single
ln -s ../build/ccutil/guides-server/build/tmp/en-US/html-single _site/guides-server-single

rm -rf _site/guides-server-multi
ln -s ../build/ccutil/guides-server/build/tmp/en-US/html _site/guides-server-multi

