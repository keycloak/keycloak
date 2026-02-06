# Test Framework Extensions

The test framework consists of multiple parts:

* JUnit Extension
* Registry
* Core Extensions
* Extensions

Each extension the test framework contains:

* Annotations
* Suppliers
* Injectable values

## Writing an extension

Create a Maven module with an implementation of `org.keycloak.testframework.TestFrameworkExtension`, and list all
available suppliers in the extension by implementing the `suppliers` method.

`valueTypeAliases` allows providing an alias for the value type supplied by a supplier. For example the core extension
maps `KeycloakServer` to `server`. This allows setting the supplier using `KC_TEST_SERVER` instead of 
`KC_TEST_KEYCLOAKSERVER`.

`alwaysEnabledValueTypes` allows providing a list of value types that are always requested by a test, regardless if 
they are requested or not.

Register the extension as a service provider by adding 
`META-INF/services/org.keycloak.testframework.TestFrameworkExtension`. The contents should be the fully qualified 
name of the `TestFrameworkExtension` implementation.

## Writing suppliers

A supplier provides instances used by the registry when injecting values into tests. To write a supplier implement
`org.keycloak.testframework.injection.Supplier`, for example:

```java
public class MySupplier implements Supplier<MyValue, InjectMyValue> {

    MyValue getValue(InstanceContext<T, S> instanceContext) {
        return new MyValue();
    }

    ...
}
```

Methods that can be implemented for a supplier:

* `close` - Used to do any cleanup required when an instance is closed
* `getAlias` - Add an alias for the supplier, by default the short name of the class is used
* `compatible` - Used to compare if the current instance is compatible with the requested instance. Usually implemented
  by comparing the annotation used to create the current instance, with the annotation for the requested instance. If
  this returns `false` the registry will destroy the current instance and create a new instance
* `getDependencies` - List all dependencies used by the supplier

Additional methods that are usually implemented by a supplier and should be used sparingly:

* `getRef` - usually not implemented, by default will use `ref` from the annotation
* `getLifeCycle` - usually not implemented, by default will use `lifecycle` from the annotation, or the default lifecycle
* `getDefaultLifecycle` - Defaults to `CLASS`, implement if the supplier should use a different lifecycle by default
* `onBeforeEach` - Executed before each test method when a value is re-used
* `order` - usually not implemented, but can be used to enforce a supplier from being called at a certain point

### Configuring using annotations

Most suppliers allow configuring the instance using annotations, for example:

```java
MyValue getValue(InstanceContext<MyValue, MyAnnotation> instanceContext) {
    MyAnnotation annotation = instanceContext.getAnnotation();
    return new MyValue(annotation.configurableValue);
}
```

### Dependencies

A supplier can have dependencies on other values. First step is to declare the dependency in the `getDependencies` then
the value can be retrieved in the `getValue` method to be injected into the instance created by the supplier. For example:

```java
MyValue getValue(InstanceContext<MyValue, MyAnnotation> instanceContext) {
    MyDependency dependency = instanceContext.getDependency(MyDependency.class);
    return new MyValue(dependency);
}
```

## Supplier configuration

#### Set the supplier

The active supplier for a given value type is configured with `KC_TEST_<value type alias>`. For example
`KC_TEST_BROWSER=chrome` results in the `ChromeWebDriverSupplier` being used to inject `WebDriver` instances.

`WebDriver` has an alias set to `browser` in the core test extension, and `ChromeWebDriverSupplier` has the alias
`chrome`. Without the aliases the above example would be `KC_TEST_WEBDRIVER=ChromeWebDriverSupplier`.

If the supplier has not been specified the default supplier is used. The default supplier is the first supplier returned
by extensions.

#### Setting included suppliers

Suppliers can be included by using `KC_TEST_<value type alias>_SUPPLIERS_INCLUDED`. If not specified all suppliers
are included.

This can be used for example for a test module that only supports a limit number of suppliers for a given type. 

#### Setting excluded suppliers

Suppliers can be excluded by using `KC_TEST_<value type alias>_SUPPLIERS_EXCLUDED`. If not specified no suppliers are
included.

This can be used for example for a test module that does not support a specific supplier for a given type.