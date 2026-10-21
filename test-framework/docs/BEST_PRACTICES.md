# Cleanup

The test framework in general re-uses as much as possible. This is important from a performance perspective. For example
if each test method started the Keycloak server and created a realm with all required configuration it would take a lot
longer to run.

One downside to this is test methods need to leave resources in a clean state. In general this is done by declaring 
cleanup on the realm at the start of the test method, instead of using try/catch blocks.

In general, it's best to create realms with `lifecycle=class` and have each test method cleanup resources within the 
realm as needed.

However, if there is a lot of cleanup required this will both be expensive and will result in less readable code.

If a lot of cleanup is required after all test methods it will typically be better to use `lifecycle=method` for the
realm.

If only some test methods require cleanup `ManagedRealm` provides methods to add or update resource within the realm
that are automatically cleaned-up after the test method completes. It is also possible to mark the realm as dirty 
within some test methods that will result in the realm being re-created after that specific test method.

The same applies to resources within the realm. For example if tests modify a user a lot it may be better to have a 
realm with `lifecycle=class`, but then have a user with `lifecycle=method`.

# Static utility classes

Static utility classes can be hard to discover. In many cases it is better to write suppliers to
provide injectable instances.

In many cases injectable instances are wrapped in a managed object that provides additional convenience methods. For
example `@InjectRealm` injects `ManagedRealm` instead of the `RealmResource` directly, as this allows providing simple
to discover additional methods.

# Asserting events

Avoid asserting events by using `Assertions` directly, and use `AssertEvents` or `AssertAdminEvents`.

# Abstract classes

It is fine to introduce an abstract class when many tests need the same injected values or configuration. This should
only be used to a limited extent as better approaches can be:

* Define concrete classes for example for `KeycloakServerConfig` and `RealmConfig` that can be re-used by multiple tests
* Create classes suffix with `Assertion` that provides convenience methods to assert complex objects

# Run-on-Server

A good tip for using run-on-server is defining re-usable functions in separate classes instead of using methods or inline
lambda in the test itself. This reduces what the objects that needs to be sent to the server, and allows re-use.

# Asserting Exceptions

Use `Assertions.assertThrows` instead of try/catch blocks.