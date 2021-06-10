package org.keycloak.testsuite.error;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.javascript.XMLHttpRequest;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST2;

public class CorsErrorResponseTest extends AbstractKeycloakTest {

    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @ArquillianResource
    @JavascriptBrowser
    protected JavascriptExecutor jsExecutor;

    @Page
    @JavascriptBrowser
    protected AuthRealm realmPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @BeforeClass
    public static void checkPreconditions() {

        ContainerAssume.assumeAuthServerSSL();

        Assume.assumeTrue("DNS resolver cannot resolve *.nip.io hosts", "127.0.0.1".equals(getAddressByName("127-0-0-1.nip.io")));
        Assume.assumeTrue(
                "This test requires that Auth Server is accessible from two different hostnames to simulate Cross-Origin requests",
                !AUTH_SERVER_HOST.equals(AUTH_SERVER_HOST2) && AUTH_SERVER_HOST.endsWith(".nip.io") && AUTH_SERVER_HOST2.endsWith(".nip.io")
        );

        if ("phantomjs".equalsIgnoreCase(System.getProperty("js.browser"))) {
            String cliArgs = System.getProperty("keycloak.phantomjs.cli.args");
            Assume.assumeTrue("This test doesn't make sense if browser CORS features are disabled", cliArgs != null && cliArgs.contains("--web-security=true"));
        }

    }

    @Before
    public void setupTest() {
        jsDriver.manage().timeouts().setScriptTimeout(WaitUtils.PAGELOAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        // Navigate browser to secondary Auth Server hostname to simulate Cross-Origin XHR requests
        jsDriver.get(
                masterRealmPage
                        .toString().replace(AUTH_SERVER_HOST, AUTH_SERVER_HOST2) +
                        "/testing/javascript/index.html"
        );
    }

    @Test
    public void getExistingRealm() {

        realmPage.setAuthRealm(AuthRealm.MASTER);

        XMLHttpRequest req = XMLHttpRequest.create()
                .method("GET")
                .url(realmPage.toString())
                .jsonResponse();

        Map<String, Object> res = req.send(jsExecutor);
        Map<String, Object> json = (Map<String, Object>) res.get("json");

        MatcherAssert.assertThat(res, IsMapContaining.hasEntry("status", 200L));
        MatcherAssert.assertThat(json, IsMapContaining.hasEntry("realm", realmPage.getAuthRealm()));

    }

    @Test
    public void getExistingRealmWithCredentials() {

        realmPage.setAuthRealm(AuthRealm.MASTER);

        XMLHttpRequest req = XMLHttpRequest.create()
                .method("GET")
                .url(realmPage.toString())
                .withCredentials()
                .jsonResponse();

        Map<String, Object> res = req.send(jsExecutor);
        Map<String, Object> json = (Map<String, Object>) res.get("json");

        MatcherAssert.assertThat(res, IsMapContaining.hasEntry("status", 200L));
        MatcherAssert.assertThat(json, IsMapContaining.hasEntry("realm", realmPage.getAuthRealm()));

    }

    @Test
    public void getInvalidRealm() {

        realmPage.setAuthRealm("invalid");

        XMLHttpRequest req = XMLHttpRequest.create()
                .method("GET")
                .url(realmPage.toString())
                .jsonResponse();

        Map<String, Object> res = req.send(jsExecutor);
        Map<String, Object> json = (Map<String, Object>) res.get("json");

        // simple request should be allowed by CORS
        MatcherAssert.assertThat(res, IsMapContaining.hasEntry("status", 404L));
        MatcherAssert.assertThat(json, IsMapContaining.hasEntry("error", "invalid_request"));
        MatcherAssert.assertThat(json, IsMapContaining.hasEntry("error_description", "Realm does not exist"));

    }

    @Test
    public void getInvalidRealmWithCredentials() {

        realmPage.setAuthRealm("invalid");

        XMLHttpRequest req = XMLHttpRequest.create()
                .method("GET")
                .url(realmPage.toString())
                .withCredentials()
                .jsonResponse();

        Map<String, Object> res = req.send(jsExecutor);
        Map<String, Object> json = (Map<String, Object>) res.get("json");

        // request with credentials should be blocked by CORS
        MatcherAssert.assertThat(res, IsMapContaining.hasEntry("status", 0L));

    }

    private static String getAddressByName(String hostname) {
        try {
            return InetAddress.getByName(hostname).getHostAddress();
        } catch (Throwable e) {
            return null;
        }
    }

}
