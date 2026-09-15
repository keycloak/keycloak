#!/usr/bin/env bash

set -o pipefail
DOCKER=docker

# use userns-remap default to map users for systemd
sudo bash -c 'cat << EOF > /etc/docker/daemon.json
{ "userns-remap": "default" }
EOF'
sudo systemctl restart docker

# chown file system to the mapper user 100000
sudo chown -R 100000 "$HOME/.m2"
sudo chown -R 100000 "$1"

echo "Starting ipa-server container"
container=$($DOCKER run --detach --rm -h ipa.example.test --sysctl net.ipv6.conf.all.disable_ipv6=0 --workdir /github/workspace -v "$1":"/github/workspace" -v "$HOME/.m2":"/root/.m2" freeipa/freeipa-server:rocky-9 ipa-server-install --unattended --realm=EXAMPLE.TEST --ds-password=password --admin-password=password --idstart=60000)

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

echo "Tests executed with result: $result"
exit $result
