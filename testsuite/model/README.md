Model testsuite
===============

Model testsuite runs tests on raw `KeycloakSessionFactory` which is
initialized only with those providers that are explicitly enabled
in a used profile via `keycloak.model.parameters` system property.

This allows writing tests and running those in different
configurations quickly.

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


