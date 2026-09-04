package org.keycloak.testsuite.cookies;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SetDefaultProvider(spi = "cookie", providerId = "default", config = {"cookie-prefix", "KC26_"})
public class CookiePrefixTest extends AbstractKeycloakTest {

    private static final String PREFIX = "KC26_";

    private KeycloakTestingClient testing;
    private SetHeaderFilter filter;

    @Before
    public void before() {
        filter = new SetHeaderFilter();
        String serverUrl = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth";
        testing = createTestingClient(serverUrl);
    }

    @After
    public void after() {
        testing.close();
    }

    @Test
    public void testPrefixedCookieNames() {
        Response response = testing.server("master").runWithResponse(session -> {
            CookieProvider cookies = session.getProvider(CookieProvider.class);
            cookies.set(CookieType.AUTH_SESSION_ID, "my-auth-session-id");
            cookies.set(CookieType.IDENTITY, "my-identity", 333);
            cookies.set(CookieType.SESSION, "my-session", 444);
            cookies.set(CookieType.LOCALE, "my-locale");
            cookies.set(CookieType.LOGIN_HINT, "my-username");
        });

        Map<String, NewCookie> cookies = response.getCookies();

        // Verify all cookies have the prefix
        Assert.assertNotNull("Expected prefixed AUTH_SESSION_ID", cookies.get(PREFIX + "AUTH_SESSION_ID"));
        Assert.assertNotNull("Expected prefixed KEYCLOAK_IDENTITY", cookies.get(PREFIX + "KEYCLOAK_IDENTITY"));
        Assert.assertNotNull("Expected prefixed KEYCLOAK_SESSION", cookies.get(PREFIX + "KEYCLOAK_SESSION"));
        Assert.assertNotNull("Expected prefixed KEYCLOAK_LOCALE", cookies.get(PREFIX + "KEYCLOAK_LOCALE"));
        Assert.assertNotNull("Expected prefixed KEYCLOAK_REMEMBER_ME", cookies.get(PREFIX + "KEYCLOAK_REMEMBER_ME"));

        // Verify unprefixed names are NOT present
        Assert.assertNull("Unprefixed AUTH_SESSION_ID should not exist", cookies.get("AUTH_SESSION_ID"));
        Assert.assertNull("Unprefixed KEYCLOAK_IDENTITY should not exist", cookies.get("KEYCLOAK_IDENTITY"));
        Assert.assertNull("Unprefixed KEYCLOAK_SESSION should not exist", cookies.get("KEYCLOAK_SESSION"));

        // Verify cookie values are correct
        Assert.assertEquals("my-auth-session-id", cookies.get(PREFIX + "AUTH_SESSION_ID").getValue());
        Assert.assertEquals("my-identity", cookies.get(PREFIX + "KEYCLOAK_IDENTITY").getValue());
        Assert.assertEquals("my-session", cookies.get(PREFIX + "KEYCLOAK_SESSION").getValue());
        Assert.assertEquals("my-locale", cookies.get(PREFIX + "KEYCLOAK_LOCALE").getValue());
        Assert.assertEquals("my-username", cookies.get(PREFIX + "KEYCLOAK_REMEMBER_ME").getValue());
    }

    @Test
    public void testPrefixedCookieGet() {
        // Send a prefixed cookie in the request header
        filter.setHeader("Cookie", PREFIX + "AUTH_SESSION_ID=prefixed-session-id;");

        testing.server("master").run(session -> {
            CookieProvider cookies = session.getProvider(CookieProvider.class);
            String value = cookies.get(CookieType.AUTH_SESSION_ID);
            Assert.assertEquals("prefixed-session-id", value);
        });
    }

    @Test
    public void testPrefixedCookieGetIgnoresUnprefixed() {
        // Send an unprefixed cookie - should NOT be found since prefix is configured (no fallback)
        filter.setHeader("Cookie", "AUTH_SESSION_ID=unprefixed-session-id;");

        testing.server("master").run(session -> {
            CookieProvider cookies = session.getProvider(CookieProvider.class);
            String value = cookies.get(CookieType.AUTH_SESSION_ID);
            Assert.assertNull("Unprefixed cookie should not be found when prefix is configured", value);
        });
    }

    @Test
    public void testPrefixedCookieExpire() {
        filter.setHeader("Cookie", PREFIX + "AUTH_SESSION_ID=to-expire;");

        Response response = testing.server("master").runWithResponse(session -> {
            session.getProvider(CookieProvider.class).expire(CookieType.AUTH_SESSION_ID);
        });

        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie expired = cookies.get(PREFIX + "AUTH_SESSION_ID");
        Assert.assertNotNull("Expired cookie should be present in response", expired);
        Assert.assertEquals(0, expired.getMaxAge());
    }

    @Test
    public void testPrefixAppliesUniformly() {
        Response response = testing.server("master").runWithResponse(session -> {
            CookieProvider cookies = session.getProvider(CookieProvider.class);
            cookies.set(CookieType.AUTH_SESSION_ID, "v1");
            cookies.set(CookieType.AUTH_SESSION_ID_HASH, "v2");
            cookies.set(CookieType.AUTH_RESTART, "v3");
            cookies.set(CookieType.AUTH_DETACHED, "v4", 100);
            cookies.set(CookieType.IDENTITY, "v5", 200);
            cookies.set(CookieType.LOCALE, "v6");
            cookies.set(CookieType.LOGIN_HINT, "v7");
            cookies.set(CookieType.SESSION, "v8", 300);
            cookies.set(CookieType.WELCOME_CSRF, "v9");
        });

        Map<String, NewCookie> cookies = response.getCookies();

        // Verify every single cookie type is prefixed — no exclusions
        Assert.assertNotNull(cookies.get(PREFIX + "AUTH_SESSION_ID"));
        Assert.assertNotNull(cookies.get(PREFIX + "KC_AUTH_SESSION_HASH"));
        Assert.assertNotNull(cookies.get(PREFIX + "KC_RESTART"));
        Assert.assertNotNull(cookies.get(PREFIX + "KC_STATE_CHECKER"));
        Assert.assertNotNull(cookies.get(PREFIX + "KEYCLOAK_IDENTITY"));
        Assert.assertNotNull(cookies.get(PREFIX + "KEYCLOAK_LOCALE"));
        Assert.assertNotNull(cookies.get(PREFIX + "KEYCLOAK_REMEMBER_ME"));
        Assert.assertNotNull(cookies.get(PREFIX + "KEYCLOAK_SESSION"));
        Assert.assertNotNull(cookies.get(PREFIX + "WELCOME_STATE_CHECKER"));

        Assert.assertEquals(9, cookies.size());
    }

    private KeycloakTestingClient createTestingClient(String serverUrl) {
        ResteasyClientBuilder restEasyClientBuilder = KeycloakTestingClient.getRestEasyClientBuilder(serverUrl);
        ResteasyClient resteasyClient = restEasyClientBuilder.build();
        resteasyClient.register(filter);
        return KeycloakTestingClient.getInstance(serverUrl, resteasyClient);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    public static class SetHeaderFilter implements ClientRequestFilter {

        private String key;
        private String value;

        public void setHeader(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            if (key != null && value != null) {
                requestContext.getHeaders().add(key, value);
            }
        }
    }
}
