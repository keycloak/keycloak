# Test Suites

A `@Suite` can supply configuration to be used when running tests from the suite. For example:

```java
@Suite
@SelectClasses(MyTest.class)
public class MyTestSuite {

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(MyTestSuiteServerConfig.class)
                .includedSuppliers("server", "remote");
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }
}
```

The above example adds some additional Keycloak server configuration, as well as limiting what server suppliers can be used for the suite.
