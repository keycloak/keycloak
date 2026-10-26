# Static code analysis

## Checker Framework

Keycloak uses the [Checker Framework](https://checkerframework.org/) to find resources that are not closed.
The checks run in CI and are opt-in locally via the `checkerframework` Maven profile.

### Running the checks

Use JDK 21 and a clean build, as incremental builds skip checks:

    JAVA_HOME=/path/to/jdk-21/ ./mvnw clean package -DskipTests -Pcheckerframework -pl quarkus/dist -am

Test sources are not checked.
Some modules and classes are excluded until they are fixed; see the comments in the `checkerframework` profiles of the `pom.xml` files.

### Fixing findings

* Prefer try-with-resources for resources that are closed in the same method.
* Use `@Owning` on a parameter or field when the callee or the containing object becomes responsible for closing the resource.
  See [`SafeInputStream`](../server-spi-private/src/main/java/org/keycloak/connections/httpclient/SafeInputStream.java).
* Use `@NotOwning` on a method that returns a resource managed by someone else.
  See [`HttpClientProvider`](../server-spi-private/src/main/java/org/keycloak/connections/httpclient/HttpClientProvider.java).
* Use `@MustCallAlias` for wrappers, where closing the wrapper closes the wrapped resource, and `@EnsuresCalledMethods` on the `close()` method that closes owned fields.
* Contracts for third-party APIs are declared in `.mvn/checkerframework/jdk.astub`.

Annotations and suppressions must describe the real contract; don't add them only to silence a warning.
Add a comment explaining each suppression, and apply it to a single method or declaration rather than a whole class.

See the [Resource Leak Checker manual](https://checkerframework.org/manual/#resource-leak-checker) for details.
