package org.keycloak.testsuite.javascript;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.util.javascript.JSObjectBuilder;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

public class JavascriptAdapterWithNativePromisesTest extends JavascriptAdapterTest {
    @Override
    protected JSObjectBuilder defaultArguments() {
        return super.defaultArguments().add("promiseType", "native");
    }

    @Before
    public void skipOnPhantomJS() {
        Assume.assumeTrue("Native promises are not supported on PhantomJS", !"phantomjs".equals(System.getProperty("js.browser")));
    }

    @Test
    @Override
    public void reentrancyCallbackTest() {
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
                .executeAsyncScript(
                        "var callback = arguments[arguments.length - 1];" +
                                "keycloak.updateToken(60).then(function () {" +
                                "       event(\"First callback\");" +
                                "       keycloak.updateToken(60).then(function () {" +
                                "          event(\"Second callback\");" +
                                "          callback(\"Success\");" +
                                "       });" +
                                "    }" +
                                ");"
                        , (driver1, output, events) -> {
                            waitUntilElement(events).text().contains("First callback");
                            waitUntilElement(events).text().contains("Second callback");
                            waitUntilElement(events).text().not().contains("Auth Logout");
                        }
                );
    }
}
