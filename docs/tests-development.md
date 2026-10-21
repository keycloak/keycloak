## Writing tests

We focus primarily on integration/functional level tests. Unit tests are avoided and only recommended for isolated
classes such as small utils. We do not use any mocking frameworks and we will not accept any contributions that adds a 
mocking framework.

When writing tests please follow the same approach as we have taken in the other tests. There are many ways to 
test software and we have chosen ours, so please appreciate that.

The main tests are provided in testsuite/integration-arquillian/tests/base. Most server tests are here.

Integration tests can be executed from the IDE in the same way as you would run unit tests. When ran from within the
IDE an embedded Keycloak server is executed automatically.

A good test to start looking at is org.keycloak.testsuite.forms.LoginTest. It is reasonable straightforward to understand
this test.

When developing your test depending on the feature or enhancement you are testing you may find it best to add to an
existing test, or to write a test from scratch. For the latter, we recommend finding another test that is close to what 
you need and use that as a basis.

All tests don't have to be fully black box testing as the testsuite deploys a special feature to the Keycloak server
that allows running code within the server. This allows writing tests that can execute functions not exposed through
APIs as well as access data that is not exposed. For an example on how to do this look at org.keycloak.testsuite.runonserver.RunOnServerTest.

As assertion method use `org.hamcrest.MatcherAssert.assertThat` where possible with a suitable matcher included 
in the `org.hamcrest.Matchers.*` package.
It provides much better readability of test failures messages.  
Please avoid as much to use set of assertions from `org.junit.Assert.*` package.
