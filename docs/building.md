## Building from source

Ensure you have **JDK 17**, **JDK 21** or **JDK 25** and Git installed

    java -version
    git --version

Newer versions of the JDK are not supported. If you have multiple JDK versions
installed, you can specify which one to use during the build by setting the `JAVA_HOME`
environment variable (this should be the directory containing `/bin/` or `/jre/`).

    JAVA_HOME=/path/to/jdk-25/ ./mvnw clean install

Instead of using a locally installed Maven, call the Maven wrapper script `mvnw` in the main folder of the project.
This will use the Maven version which is supported by this project.

---                                                                      

First clone the Keycloak repository:
    
    git clone https://github.com/keycloak/keycloak.git
    cd keycloak

To build Keycloak without running the tests:

    ./mvnw clean install -DskipTests

To build Keycloak and run the tests (note that this might take several hours):

    ./mvnw clean install

To build Keycloak with adapters run:

    ./mvnw clean install -DskipTests -Pdistribution

To build only the server run:

    ./mvnw -pl quarkus/deployment,quarkus/dist -am -DskipTests clean install

You can then find the ZIP distribution in `quarkus/dist/target` folder.

---
**NOTE**

Classes from `org.keycloak.testsuite.*` packages aren't suitable to be used in production.

---

This project contains opt-in support for incremental Maven builds using the [Apache Maven Build Cache Extension](https://github.com/apache/maven-build-cache-extension) which is disabled by default.

To enable it for a single Maven command execution, enable it as follows: 

    ./mvnw -D -Dmaven.build.cache.enabled=true ...

To enable it by default, add it to the `MAVEN_OPTS` environment variable:

    export MAVEN_OPTS="-Dmaven.build.cache.enabled=true"

---
**NOTE**

To ensure that development in a branch does not break compatibility with existing releases, proto-schema-compatibility-maven-plugin checks may be run, which can cause builds to fail in proxy environments.
To avoid this, you can skip this check by adding the following property:

    -DskipProtoLock=true

---

### Checking for resource leaks

The `checkerframework` profile runs the Checker Framework ResourceLeakChecker.
Use JDK 21 for this profile; compatibility with newer JDKs is deferred.
Run a clean build so the checker sees all sources in the selected modules:

    JAVA_HOME=/path/to/jdk-21/ ./mvnw clean package -DskipTests -Pcheckerframework -pl quarkus/dist -am

The distribution selects its production dependencies, but coverage is incremental.
Sources under `src/test/` are excluded by the root POM's `-AskipFiles` option, including test helpers regardless of their filenames.
Tests are still compiled normally; the exclusion only skips checker diagnostics for those sources.
The Quarkus modules, `model/infinispan`, `integration/client-cli/admin-cli`, and classes named in the root POM's `-AskipDefs` option are currently excluded.
The build-time `misc/db-compatibility-verifier` Maven plugin is also excluded because it is not shipped in the server.
The class exclusions cover both confirmed checker crashes and outstanding ownership diagnostics, as detailed below.
Correct ownership annotations in excluded classes still describe contracts to checked callers, but the checker does not verify those implementations.
Modules outside the selected dependency graph, such as the integration testsuite, are not covered by this command.

Third-party ownership contracts are declared in `.mvn/checkerframework/jdk.astub`.
For example, its `@NotOwning` declaration on `SerializerProvider.getGenerator()` describes the borrowed generator for all callers, instead of suppressing a warning at each call site.

#### Class exclusions

The root POM matches fully qualified class names, including their nested classes, rather than substrings or unrelated classes with the same simple name.
Only these seven classes reproduce the raw-type `AsSuperVisitor` crash tracked in [checker-framework#8340](https://github.com/typetools/checker-framework/issues/8340):

- `ConditionalCredentialAuthenticatorFactory`
- `LoAUtil`
- `CryptoUtils`
- `AllowedProtocolMappersExecutorFactory`
- `SecureClientAuthenticatorExecutorFactory`
- `ProtocolMappersClientRegistrationPolicyFactory`
- `LinkedAccountsResource`

The other exclusions produce ordinary checker diagnostics, not that upstream crash.
They remain excluded pending ownership-contract work; fixing the upstream crash alone will not enable them:

| Excluded classes | Outstanding diagnostics |
| --- | --- |
| `DefaultHttpClientFactory` | Lazy client initialization and HTTP response/stream ownership. |
| `org.keycloak.exportimport.ExportImportManager` | Import/export initialization resource ownership. The `org.keycloak.storage.ExportImportManager` interface is not excluded. |
| `AbstractFileBasedImportProvider` | Ownership transfer to the property-replacing stream wrapper. |
| `IframeUtil`, `ThemeResource`, `WelcomeResource` | Resource ownership through suppliers and JAX-RS response entities. |
| `LDAPContextManager`, `LDAPOperationManager`, `LDAPStartTlsClose` | Resource types in `AtomicReference`, and TLS/socket cleanup contracts. |
| `MSSQLAsyncCommitIntegrator`, `PostgreSQLAsyncCommitIntegrator` | Hibernate bootstrap connection-release contracts. |
| `SSLSocketFactory`, `SniSSLSocketFactory` | Socket aliasing and inherited socket-factory contracts. |

`SniSSLSocketFactory` is outside the distribution's selected dependency graph; its diagnostics were reproduced separately.
Module-level exclusions and the test-source exclusion are independent of this class list.

#### Choosing resource annotations

Use try-with-resources for resources owned locally.
The main annotations for ownership contracts are `@Owning` and `@NotOwning`:

| Where | Annotation | Meaning |
| --- | --- | --- |
| Method or constructor parameter | `@Owning` | The callee takes responsibility for closing the resource or transferring it to another owner, for example by storing it in an owning field. |
| Instance field | `@Owning` | The containing object owns the resource and must fulfill its cleanup obligation through its own lifecycle. |
| Method declaration | `@NotOwning` | The returned resource is borrowed; the caller does not acquire responsibility for closing it. |

Constructor results are always owning, and method returns are owning by default.
Parameters and fields are non-owning by default, so they generally need no explicit `@NotOwning`.
Avoid redundant annotations and annotations on local variables when the checker can infer their types.
Ownership annotations describe declarations, not collection elements; `List<@Owning InputStream>` is not supported.

For example, [`HttpClientProvider.getHttpClient()`](../server-spi-private/src/main/java/org/keycloak/connections/httpclient/HttpClientProvider.java) uses `@NotOwning` because the provider manages the client's lifecycle.
In contrast, [`SafeInputStream`](../server-spi-private/src/main/java/org/keycloak/connections/httpclient/SafeInputStream.java) takes an `@Owning` constructor parameter and stores it in an `@Owning` field.
Its `close()` method closes that field and declares the postcondition with `@EnsuresCalledMethods(value = "delegate", methods = "close")`.

Other annotations are needed when describing cleanup obligations rather than ownership:

| Annotation | Where and when to use it |
| --- | --- |
| `@InheritableMustCall("close")` | On a resource class to require `close()` on its instances and subclasses; see [`SimpleHttpResponse`](../server-spi-private/src/main/java/org/keycloak/http/simple/SimpleHttpResponse.java). |
| `@MustCall(...)` | On a type use or class to describe its cleanup obligation; a class annotation is not inherited by subclasses. For example, `@MustCall({}) InputStream` describes a stream with no cleanup obligation, as returned by [`DocumentUtil`](../saml-core/src/main/java/org/keycloak/saml/common/util/DocumentUtil.java) for in-memory XML. |
| `@MustCallAlias` | On both a wrapper constructor and its resource parameter when closing either object fulfills the same cleanup obligation. Store the parameter in the wrapper's sole `@Owning` field and declare how the wrapper closes it. |
| `@EnsuresCalledMethods(...)` | On a cleanup method to specify which methods it calls on owned fields or parameters, as in `SafeInputStream.close()`. |
| `@CreatesMustCallFor("this")` | On a method that creates a new cleanup obligation for its receiver, for example by reopening a resource or assigning a non-final owning field. |

Declare an `@Owning` parameter only when the implementation actually assumes responsibility, not merely because it reads from the resource.
Review exceptional paths too: ownership transfer for an `@Owning` parameter is modeled on normal return, so the annotation alone does not promise cleanup when the method throws.
Do not use `@NotOwning` or `@MustCall({})` merely to silence a leak warning; they must describe the actual contract.
For third-party APIs, first check whether an ownership contract is missing from the stub file.
Reserve narrowly scoped suppressions with explanations for checker limitations that cannot be expressed by an accurate contract.
Every suppression or empty must-call obligation must have a comment explaining why it is valid.
For workarounds, include a follow-up issue or removal condition when available.
Suppress an individual method or declaration rather than an entire resource class, so unrelated ownership and cleanup errors remain checked.
Resources intentionally retained for server lifetime require a documented exemption, not a borrowed-resource contract.
See the [Resource Leak Checker manual](https://checkerframework.org/manual/#resource-leak-checker) for exception contracts and more advanced ownership patterns.

### Starting Keycloak

Follow the [`quarkus` module documentation](../quarkus/README.md#running-in-keycloak-development-mode) for instructions how to run Keycloak in dev environment.

## Working with the codebase

We don't currently enforce a code style in Keycloak, but a good reference is the code style used by WildFly. This can be 
retrieved from [Wildfly ide-configs](https://github.com/wildfly/wildfly-core/tree/main/ide-configs).To import formatting 
rules, see following [instructions](http://community.jboss.org/wiki/ImportFormattingRules).

If your changes require updates to the database read [Updating Database Schema](updating-database-schema.md).

If your changes require introducing new dependencies or updating dependency versions please discuss this first on the
dev mailing list. We do not accept new dependencies to be added lightly, so try to use what is available.

### Building project from the IDE

Some parts of the project rely on generated code using Maven plugins. These steps might be skipped when building using
IDE resulting in compilation errors. To work around this make sure to build the project first using Maven. After the
initial build with Maven you should be able to build the project using the IDE as it will use the classes previously
generated by Maven plugins. Make sure you don't rebuild the whole project using the IDE as it would delete the generated
classes. E.g. in IntelliJ IDEA use `Build → Build Project` instead of `Build → Rebuild Project`.

---
**NOTE**

If you are building the Operator from your IDE, make sure to build the project with the `operator` profile enabled in Maven
as it's excluded by default:

    ./mvnw clean install -Poperator -DskipTests

---
