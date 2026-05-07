# Debugging

When debugging Keycloak or extensions the embedded server mode is very useful as it allows debugging tests, Keycloak,
as well as extensions from the IDE without attaching remote debuggers.

To configure the embedded server mode the simplest approach is to add `.env.test` to the root of the project with the
following contents:

```properties
KC_TEST_SERVER=embedded
KC_TEST_DATABASE=dev-file
KC_TEST_DATABASE_REUSE=true
```

Using `dev-file` with re-use is optional, but helps to reduce the startup time.

## Debug helper

When debugging Keycloak or extension code there may be an issue that the relevant debug statement is invoked during
test framework setup, or during other tests. To help with this there is a utility called `DebugHelper`.

To use this when adding a breakpoint add a condition on `DebugHelper.isInTest()` this will result in the debugger
only stopping on the breakpoint if a test method is currently in progress.

For more advanced use-cases it is also possible to specify the test class and or method name the debugger should stop by
passing a string to `isInTest`. The syntax is `testClassName#testMethodName` where `testClassName` can be the simple-name
of the test class, or the fully qualified name:

* `DebugHelper.isInTest("org.package.MyTest")`
* `DebugHelper.isInTest("MyTest")`
* `DebugHelper.isInTest("#testMethod")`
* `DebugHelper.isInTest("MyTest#testMethod")`
* `DebugHelper.isInTest("org.package.MyTest#testMethod")`
