#!/bin/bash

set -o pipefail
DOCKER=podman

if [ -f "$HOME/ipa-data.tar" ]; then
  echo "Using data from previous execution"
  sudo tar xpf "$HOME/ipa-data.tar" -C "$HOME"
else
  mkdir "$HOME/ipa-data"
fi

echo "Starting ipa-server container"
container=$($DOCKER run --detach --rm -h ipa.example.test --sysctl net.ipv6.conf.all.disable_ipv6=0 --workdir /github/workspace -v "$HOME/ipa-data":"/data":Z -v "$1":"/github/workspace" -v "$HOME/.m2":"/root/.m2" freeipa/freeipa-server:rocky-9 ipa-server-install --unattended --realm=EXAMPLE.TEST --ds-password=password --admin-password=password --idstart=60000)

echo "Container $container started, waiting ipa-server configuration"
sleep 30
line=$($DOCKER logs $container | tail -1)
regexp="FreeIPA server configured.|FreeIPA server started."
while ! [[ "$line" =~ $regexp ]]; do
  sleep 30
  line=$($DOCKER logs $container | tail -1)
  if [ $? -ne 0 ]; then
    exit 1
  fi
done

new_install="false"
if [[ $line == "FreeIPA server configured." ]]; then
  new_install="true"
fi
echo "The server is ready, performing tests"
$DOCKER exec $container .github/scripts/run-ipa-tests.sh $new_install
result=$?

$DOCKER stop $container

if [ $result -eq 0 ]; then
  echo "Doing a backup of the ipa-data directory for caching"
  sudo tar cpf "$HOME/ipa-data.tar" -C "$HOME" ipa-data
fi

echo "Tests executed with result: $result"
exit $result
