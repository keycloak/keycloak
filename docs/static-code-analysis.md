# Static code analysis

## Checker Framework

Keycloak uses the [Checker Framework](https://checkerframework.org/) to find resources that are not closed.
The checks run in CI and are opt-in locally via the `checkerframework` Maven profile.

### Running the checks

Use a clean build, as incremental builds skip checks:

    ./mvnw clean package -DskipTests -Pcheckerframework -pl quarkus/dist -am

The checks run with the same JDK as the regular build and in CI.

Test sources are not checked yet.
Some modules and classes are excluded until they are fixed; see the comments in the `checkerframework` profiles of the `pom.xml` files.
The remaining class exclusions cover ownership diagnostics tracked in [keycloak/keycloak#54332](https://github.com/keycloak/keycloak/issues/54332).

### Fixing findings

Start by deciding who is responsible for closing the resource.
If you create a resource and use it only in the current method, prefer try-with-resources:

```java
try (InputStream input = Files.newInputStream(path)) {
    // Read from input.
}
```

No ownership annotations are needed here.
If the resource must remain open after the method returns, make sure responsibility passes to the code that will close it instead.

#### Who closes the resource?

`@Owning` means "this reference carries responsibility for cleanup".
`@NotOwning` means "this reference borrows the resource; responsibility stays elsewhere".
The meaning depends on where the annotation appears:

| Declaration | `@Owning` | `@NotOwning` |
| --- | --- | --- |
| Method return | The caller becomes responsible for cleanup. **This is the default.** | The caller borrows the resource; its existing owner remains responsible. |
| Parameter | The method accepts responsibility, for example by closing the resource or storing it in an owning field. | Passing the resource does not transfer responsibility. **This is the default.** |
| Instance field | The object containing the field is responsible for cleaning up the resource. | The field refers to a resource owned elsewhere. **This is the default.** |

For example, opening a stream transfers responsibility to the caller, while returning a shared client does not:

```java
InputStream open(Path path) throws IOException {
    return Files.newInputStream(path); // The caller must close the stream.
}

@NotOwning
CloseableHttpClient getSharedClient() {
    return client; // The provider closes this client during shutdown.
}
```

Use the returned stream in try-with-resources, but do not close the shared client after each use.
Do not add `@Owning` to a return just to state the default.

When an object keeps a resource for later use, mark both the parameter that accepts ownership and the field that retains it:

```java
private final @Owning InputStream input;

StreamReader(@Owning InputStream input) {
    this.input = input;
}
```

`StreamReader` must also provide cleanup that closes `input`.
An owning parameter transfers responsibility only when the method or constructor returns normally; review failure paths too.
Details are in the manual's [ownership section](https://checkerframework.org/manual/#resource-leak-ownership).

#### What cleanup is required?

Ownership says **who** is responsible; must-call annotations say **what** they must do.
For standard resources such as `InputStream`, the checker already knows that `close()` is required.

* `@MustCall("close")` describes a type whose instances require a call to `close()`.
* `@InheritableMustCall("close")` declares the requirement on a resource class and its subclasses.
  Use this when defining a custom resource whose cleanup obligation must be inherited.
* `@MustCall({})` means no cleanup call is required, for example for an in-memory stream whose `close()` does nothing.
  It does **not** mean someone else will close the resource; use `@NotOwning` for a borrowed reference instead.

For an object with an owning field, its cleanup method must close that field.
`@EnsuresCalledMethods` tells the checker which field was cleaned up:

```java
@Override
@EnsuresCalledMethods(value = "input", methods = "close")
public void close() throws IOException {
    input.close();
}
```

For wrappers where closing either the wrapper or the underlying resource fulfills the same obligation, `@MustCallAlias` describes that relationship.
It must appear on both the wrapper constructor and its resource parameter.
Use the manual's [complete wrapper example](https://checkerframework.org/manual/#resource-leak-wrapper-type-example) when implementing this less common case.

#### Missing contracts and suppressions

If a third-party API returns a shared resource or takes ownership of an argument, declare that contract in `.mvn/checkerframework/jdk.astub`.
This lets all callers benefit from the same contract instead of adding suppressions at each call site.

Annotations and suppressions must describe the real contract; don't add them only to silence a warning.
Use a suppression only when correct cleanup cannot be expressed to the checker.
Use Java's `@SuppressWarnings` with the diagnostic name from the checker output, for example `@SuppressWarnings("required.method.not.called")`.
This disables that check for the annotated declaration; it does not close the resource or transfer ownership.
Apply it to a single method or declaration rather than a whole class, and comment why it is safe.
Also explain empty must-call annotations, and include a follow-up issue or removal condition for temporary workarounds.

The suppressions relevant to the current stream checks are:

| Diagnostic name | What the checker cannot verify |
| --- | --- |
| `required.method.not.called` | The stream is closed before it goes out of scope, or responsibility for closing it passes to another owner. |
| `type.argument` | A stream used as a generic type argument, such as in `List<InputStream>`, meets the generic type's resource requirements. |
| `type.arguments.not.inferred` | Generic type arguments can be inferred when creating a collection of streams, for example with `new ArrayList<>()`. |

These are diagnostic names, not separate annotations.
Use only the specific name reported for the code you have reviewed; none of these diagnostics automatically means the checker is wrong.
The collection suppressions address the checker's inability to track ownership of individual stream elements.
They are valid only when the collection's owner actually closes every stream.
Missing cleanup or an incorrect contract must still be fixed.

See the [Resource Leak Checker manual](https://checkerframework.org/manual/#resource-leak-checker) for details.

### Troubleshooting

#### `[required.method.not.called]`

**Observation:** the build fails with an error like this, pointing to a line of code:

    SamlService.java:[1213,83] error: [required.method.not.called] $$ 1213 $$ method close $$ SamlProtocolUtils.limit(inputStream, maxSize) $$ ...

The checker 4.4.0 prints only these arguments instead of a message text, see [typetools/checker-framework#8562](https://github.com/typetools/checker-framework/issues/8562).
This section needs an update once the issue is fixed in a released version.

**Cause:** the expression at that line creates or returns a resource, for example a stream, that is neither closed nor handed over to a new owner.
The position is where the resource is created, not where it should have been closed.
A typical case is a stream that is created inline and passed to a method whose parameter is not `@Owning`.

**Fix:** close the resource, for example with try-with-resources:

```java
// Fails: the LimitedInputStream is never closed.
Document soapBodyContents = Soap.extractSoapMessage(SamlProtocolUtils.limit(inputStream, maxSoapMessageRequestSize));

// Fixed: close it when done.
try (InputStream limitedInputStream = SamlProtocolUtils.limit(inputStream, maxSoapMessageRequestSize)) {
    soapBodyContents = Soap.extractSoapMessage(limitedInputStream);
}
```

Fix it as described in [Fixing findings](#fixing-findings), and use a suppression only as the last resort.

#### `Cannot attach type annotations ... class file for <class> not found`

**Observation:** the build fails with an error that names a class in an upstream module's JAR instead of a source file, and the missing class on the second line:

```
keycloak-server-spi-private.jar(/org/keycloak/authorization/fgap/AdminPermissionsSchema.class): error: Cannot attach type annotations @org.checkerframework.checker.mustcall.qual.MustCall({}),...
  class file for jakarta.persistence.criteria.CriteriaQuery not found
```

**Cause:** with JDK 22 and later, a module must have all classes that the checked upstream modules use in their method signatures on its classpath, even if it never calls those methods.
Dependencies with `provided` scope are not passed on to downstream modules, so such a class is missing there.
This is expected and will not be fixed in the Checker Framework, see [typetools/checker-framework#8346](https://github.com/typetools/checker-framework/issues/8346).

**Fix:** declare a dependency that provides the missing class in the failing module, only in a `checkerframework` profile of its `pom.xml`, so regular builds are not changed:

```xml
<profile>
    <id>checkerframework</id>
    <dependencies>
        <!-- Required by the type annotations the Checker Framework writes into upstream bytecode, see https://github.com/typetools/checker-framework/issues/8346 -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</profile>
```

#### `Compilation failure` without any error details

**Observation:** building a single module with `-Pcheckerframework -pl <module>` fails with `Compilation failure`, and no error is shown.

**Cause:** the `checkerframework` profile runs the compiler in a forked process, and Maven then doesn't show the compiler's error in this case.

**Fix:** run `./mvnw -X compile -Pcheckerframework -pl <module>` to get the compiler options and classpath, and run `javac` with them to see the error.
Usually a dependency is missing, see the previous entry.
