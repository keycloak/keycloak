Model testsuite
===============

Model testsuite runs tests on raw `KeycloakSessionFactory` which is
initialized only with those providers that are explicitly enabled
in a used profile via `keycloak.model.parameters` system property.

This allows writing tests and running those in different
configurations quickly, e.g. once with legacy JPA storage,
second time with purely new Hot Rod implementation.

The valid parameters are names of classes in `org.keycloak.testsuite.model.parameters`
package, and it is possible to combine those by providing multiple class names
separated by comma.

To simplify matters, common combinations of parameters are defined as maven profiles.


Test coverage
-------------

To see test coverage via jacoco, set `jacoco.skip` property to `false`, e.g.:

```
mvn test -Pjpa -Dtest=ClientModelTest -Djacoco.skip=false
```

Then you can generate the coverage report by using the following command:

```
mvn org.jacoco:jacoco-maven-plugin:0.8.7:report \
    -Djacoco.skip=false -Djacoco.dataFile=target/jacoco.exec
```

The test coverage report is then available from `target/site/jacoco/index.html` file.

Profiling
---------

If you have [Async Profiler](https://github.com/jvm-profiling-tools/async-profiler/)
installed, you can generate flame graphs of the test run for profiling purposes.
To do so, you set `libasyncProfilerPath` system property to the location of the
async profiler library:

```
mvn test -Pjpa -Dtest=ClientModelTest \
    -DlibasyncProfilerPath=/usr/local/async-profiler/build/libasyncProfiler.so 
```

The results are available in the `target/profile.html` file.

Usage of Testcontainers
-----------------------

Some profiles within model tests require running 3rd party software, for
example, database or Infinispan. For running these we are using
[Testcontainers](https://www.testcontainers.org/). This may require some
additional configuration of your container engine.

#### Podman settings

For more details see the following [Podman guide from Quarkus webpage](https://quarkus.io/guides/podman).

Specifically, these steps are required:
```shell
# Enable the podman socket with Docker REST API (only needs to be done once)
systemctl --user enable podman.socket --now

# Set the required environment variables (need to be run everytime or added to profile)
export DOCKER_HOST=unix:///run/user/${UID}/podman/podman.sock
```

Testcontainers are using [ryuk](https://hub.docker.com/r/testcontainers/ryuk)
to cleanup containers after tests. To make this work with Podman add the
following line to `~/.testcontainers.properties`
```shell
ryuk.container.privileged=true
```
Alternatively, disable usage of ryuk (using this may result in stale containers
still running after tests finish. This is not recommended especially if you are
executing tests from Intellij IDE as it [may not stop](https://youtrack.jetbrains.com/issue/IDEA-190385) 
the containers created during test run).
```shell
export TESTCONTAINERS_RYUK_DISABLED=true #not recommended - see above!
```

#### Docker settings

To use Testcontainers with Docker it is necessary to
[make Docker available for non-root users](https://docs.docker.com/engine/install/linux-postinstall/).

Running HotRod tests with external Infinispan
---------------------------------------------

By default, Model tests with `hot-rod` profile spawn a new Infinispan container
with each test execution. It is also possible, to configure Model tests to
connect to an external instance of Infinispan. To do so, execute tests with
the following command:
```shell
mvn test -Phot-rod \
  -Dhot-rod.start-container=false \
  -Dhot-rod.connection.host=<host> \
  -Dhot-rod.connection.port=<port> \
  -Dhot-rod.connection.username=<username> \
  -Dhot-rod.connection.password=<password>
```
