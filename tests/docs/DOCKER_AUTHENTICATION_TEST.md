# Docker Authentication test

The `DockerClientTest` integration test validates Docker registry authentication against Keycloak.
It lives in the `tests/base` module and uses the Keycloak Test Framework.

## Prerequisites

Validate that your machine has a working Docker installation that is accessible to the JVM running the test.
The exact setup depends on the operating system.

By default, the test framework runs in `distribution` mode and expects a built Keycloak distribution ZIP.
Build it once from the repository root:

    ./mvnw -pl quarkus/deployment,quarkus/dist -am -DskipTests clean install

To run without building a distribution, use embedded mode instead:

    KC_TEST_SERVER=embedded

See [Running tests](../../test-framework/docs/RUNNING_TESTS.md#server-type) for details on server modes.

## General guidelines

If you're running a Docker fork that always lists a host component of an image on `docker images` (e.g. Fedora / RHEL Docker),
use `-Ddocker.io-prefix-explicit=true` when running the test.

The test starts registry and Docker-in-Docker containers with host networking (`--network=host`).
This works on Linux with Docker or Podman. macOS verification is welcome; Docker Desktop does not support host networking the same way Linux does.

## Fedora

On Fedora one way to set up Docker server is the following:

    # install docker
    sudo dnf install docker

    # configure docker
    # remove --selinux-enabled from OPTIONS
    sudo vi /etc/sysconfig/docker

    # create docker group and add your user (so docker wouldn't need root permissions)
    sudo groupadd docker && sudo gpasswd -a ${USER} docker && sudo systemctl restart docker
    newgrp docker

    # you need to login again after this

    # make sure Docker is available
    docker pull registry:2

You may also need to add an iptables rule to allow container to host traffic:

    sudo iptables -I INPUT -i docker0 -j ACCEPT

Then, run the test passing `-Ddocker.io-prefix-explicit=true`:

    ./mvnw -f tests/base/pom.xml \
        clean test \
        -Dtest=DockerClientTest \
        -Ddocker.io-prefix-explicit=true

To skip the distribution build, add `KC_TEST_SERVER=embedded` to the command.

## macOS

On macOS install Docker for Mac, start it, and check that it works:

    # make sure Docker is available
    docker pull registry:2

Be especially careful to restart Docker after every sleep / suspend to ensure the Docker VM system clock is synchronized with the host.

Then, run the test:

    ./mvnw -f tests/base/pom.xml \
        clean test \
        -Dtest=DockerClientTest

To skip the distribution build, add `KC_TEST_SERVER=embedded` to the command.
