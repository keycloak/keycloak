# Getting started

## Configuring Maven

Update your `pom.xml` to include the following (replace `KEYCLOAK_VERSION` with the latest Keycloak release):

```xml
    <properties>
        <keycloak.version>KEYCLOAK_VERSION</keycloak.version>
        <testframework.surefire.args>
            -XX:+ExitOnOutOfMemoryError
            -XX:+HeapDumpOnOutOfMemoryError
            -Djava.util.logging.manager=org.jboss.logmanager.LogManager
            -Djava.util.concurrent.ForkJoinPool.common.threadFactory=io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory
        </testframework.surefire.args>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.keycloak.testframework</groupId>
                <artifactId>keycloak-test-framework-bom</artifactId>
                <version>${keycloak.version}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <dependencies>
        <dependency>
            <groupId>org.keycloak.testframework</groupId>
            <artifactId>keycloak-test-framework-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>${testframework.surefire.args}</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

This will import dependency versions from the test framework BOM. It will also configure the Surefire plugin to pass 
some arguments to the JVM that are required by the test framework to run properly.

## Writing a test

To write a test that uses the new test framework, annotate the test class with `@KeycloakIntegrationTest`, which will start a 
Keycloak server when the tests are executed. You can also inject various resources for example creating a realm for the 
tests. For example:

```java
@KeycloakIntegrationTest
public class MyProviderTest {
    
    @InjectRealm
    ManagedRealm myrealm;

    @Test
    public void myTest() {
        Assertions.assertNotNull(myrealm.admin().toRepresentation());
    }
}
```

## Running tests

Tests can be executed from Maven or directly from your favourite IDE. For example the following command will run the 
tests using the default test framework settings:

```shell
mvn test -Dtest=MyProviderTest
```
