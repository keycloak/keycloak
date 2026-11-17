package org.keycloak.tests.cookies;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.cookie.CookieType;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.cookies.CookieAssertions.assertCookie;
import static org.keycloak.tests.cookies.CookieAssertions.assertSetCookieCount;
import static org.keycloak.tests.cookies.CookieAssertions.getSetCookieHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
class DefaultCookieProviderTest {

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    void testCookieDefaults() throws IOException {
        HttpPost post = new HttpPost(cookieTestingUri("set-all"));
        try (CloseableHttpResponse response = execute(post)) {
            assertSetCookieCount(response, 9);
            assertCookie(response, CookieType.AUTH_SESSION_ID, "my-auth-session-id", "/realms/master/", -1, true, true, "None", true);
            assertCookie(response, CookieType.AUTH_SESSION_ID_HASH, "my-kc-auth-session", "/realms/master/", 60, true, false, "None", true);
            assertCookie(response, CookieType.AUTH_RESTART, "my-auth-restart", "/realms/master/", -1, true, true, "None", false);
            assertCookie(response, CookieType.AUTH_DETACHED, "my-auth-detached", "/realms/master/", 222, true, true, "Strict", false);
            assertCookie(response, CookieType.IDENTITY, "my-identity", "/realms/master/", 333, true, true, "None", true);
            assertCookie(response, CookieType.LOCALE, "my-locale", "/realms/master/", -1, true, true, "None", false);
            assertCookie(response, CookieType.LOGIN_HINT, "my-username", "/realms/master/", 31536000, true, true, "None", false);
            assertCookie(response, CookieType.SESSION, "my-session", "/realms/master/", 444, true, false, "None", true);
            assertCookie(response, CookieType.WELCOME_CSRF, "my-welcome-csrf", "/realms/master/cookie-testing/set-all", 300, true, true, "Strict", false);
        }
    }

    @Test
    void testCookieDefaultsWithInsecureContext() throws IOException {
        HttpPost post = new HttpPost(insecureCookieTestingUri());
        try (CloseableHttpResponse response = execute(post)) {
            assertSetCookieCount(response, 9);
            assertCookie(response, CookieType.AUTH_SESSION_ID, "my-auth-session-id", "/realms/master/", -1, false, true, "Lax", true);
            assertCookie(response, CookieType.AUTH_SESSION_ID_HASH, "my-kc-auth-session", "/realms/master/", 60, false, false, "Lax", true);
            assertCookie(response, CookieType.AUTH_RESTART, "my-auth-restart", "/realms/master/", -1, false, true, "Lax", false);
            assertCookie(response, CookieType.AUTH_DETACHED, "my-auth-detached", "/realms/master/", 222, false, true, "Strict", false);
            assertCookie(response, CookieType.IDENTITY, "my-identity", "/realms/master/", 333, false, true, "Lax", true);
            assertCookie(response, CookieType.LOCALE, "my-locale", "/realms/master/", -1, false, true, "Lax", false);
            assertCookie(response, CookieType.LOGIN_HINT, "my-username", "/realms/master/", 31536000, false, true, "Lax", false);
            assertCookie(response, CookieType.SESSION, "my-session", "/realms/master/", 444, false, false, "Lax", true);
            assertCookie(response, CookieType.WELCOME_CSRF, "my-welcome-csrf", "/realms/master/cookie-testing/set-all", 300, false, true, "Strict", false);
        }
    }

    @Test
    void testSessionCookieValue() throws IOException {
        String sessionValue = "my-realm/5256327f-049f-4d01-acb9-68c7936bdeb3/c6cd1f10-40ab-44c0-b77b-6028350d8564";

        HttpPost post = new HttpPost(cookieTestingUri("set-session") + "?value=" + sessionValue + "&maxAge=444");
        try (CloseableHttpResponse response = execute(post)) {
            String setHeader = getSetCookieHeader(response, CookieType.SESSION.getName());
            assertThat("Set-Cookie header for " + CookieType.SESSION.getName(), setHeader,
                    startsWith(CookieType.SESSION.getName() + "=\"" + sessionValue + "\";"));
        }

        HttpGet get = new HttpGet(cookieTestingUri("get") + "?type=" + CookieType.SESSION.getName());
        get.setHeader("Cookie", CookieType.SESSION.getName() + "=\"" + sessionValue + "\";");
        try (CloseableHttpResponse response = execute(get)) {
            assertThat("Parsed cookie value", EntityUtils.toString(response.getEntity()), equalTo(sessionValue));
        }
    }

    @Test
    void testExpire() throws IOException {
        HttpPost post = new HttpPost(cookieTestingUri("expire") + "?type=" + CookieType.AUTH_SESSION_ID.getName() + "&type=" + CookieType.LOCALE.getName());
        post.setHeader("Cookie", CookieType.AUTH_SESSION_ID.getName() + "=new;" + CookieType.AUTH_RESTART.getName() + "=new;");
        try (CloseableHttpResponse response = execute(post)) {
            assertSetCookieCount(response, 1);
            assertCookie(response, CookieType.AUTH_SESSION_ID, "", "/realms/master/", 0, false, false, null, false);
        }
    }

    @Test
    void testCookieHeaderWithSpaces() throws IOException {
        HttpGet get = new HttpGet(cookieTestingUri("get") + "?type=" + CookieType.AUTH_SESSION_ID.getName());
        get.setHeader("Cookie", "terms_user=; KC_RESTART=eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJhZDUyMjdhMy1iY2ZkLTRjZjAtYTdiNi0zOTk4MzVhMDg1NjYifQ.eyJjaWQiOiJodHRwczovL3Nzby5qYm9zcy5vcmciLCJwdHkiOiJzYW1sIiwicnVyaSI6Imh0dHBzOi8vc3NvLmpib3NzLm9yZy9sb2dpbj9wcm92aWRlcj1SZWRIYXRFeHRlcm5hbFByb3ZpZGVyIiwiYWN0IjoiQVVUSEVOVElDQVRFIiwibm90ZXMiOnsiU0FNTF9SRVFVRVNUX0lEIjoibXBmbXBhYWxkampqa2ZmcG5oYmJoYWdmZmJwam1rbGFqbWVlb2lsaiIsInNhbWxfYmluZGluZyI6InBvc3QifX0.d0QJSOQ6pJGzqcjqDTRwkRpU6fwYeICedL6R9Gqs8CQ; AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d;AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d;AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d4;");
        try (CloseableHttpResponse response = execute(get)) {
            assertThat("Status code", response.getStatusLine().getStatusCode(), equalTo(200));
            assertThat("Parsed cookie value", EntityUtils.toString(response.getEntity()),
                    equalTo("55000981-8b5e-4c8d-853f-ee4c582c1d0d4"));
        }
    }

    @Test
    void testExpireOldUnused() throws IOException {
        HttpGet get = new HttpGet(cookieTestingUri("get") + "?type=" + CookieType.AUTH_SESSION_ID.getName());
        get.setHeader("Cookie", CookieType.OLD_UNUSED_COOKIES[0].getName() + "=legacy; "
                + CookieType.OLD_UNUSED_COOKIES[1].getName() + "=legacy; "
                + CookieType.OLD_UNUSED_COOKIES[2].getName() + "=ignore");
        try (CloseableHttpResponse response = execute(get)) {
            assertThat("Status code", response.getStatusLine().getStatusCode(), equalTo(204));
            assertSetCookieCount(response, 3);

            assertCookie(response, CookieType.OLD_UNUSED_COOKIES[0], "", "/realms/master/", 0, false, false, null, false);
            assertCookie(response, CookieType.OLD_UNUSED_COOKIES[1], "", "/realms/master/", 0, false, false, null, false);
            assertCookie(response, CookieType.OLD_UNUSED_COOKIES[2], "", "/realms/master/", 0, false, false, null, false);
        }
    }

    @Test
    void testCustomCookie() throws IOException {
        HttpPost post = new HttpPost(cookieTestingUri("set-custom") + "?name=mycookie&value=myvalue&maxAge=1232");
        try (CloseableHttpResponse response = execute(post)) {
            assertSetCookieCount(response, 1);
            assertCookie(response, "mycookie", "myvalue", "/realms/master/cookie-testing/set-custom", 1232, false, false, null, false);
        }
    }

    @Test
    void testSafariQuirks() throws IOException {
        HttpPost post = new HttpPost(cookieTestingUri("set-all"));
        post.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Safari/605.1.15");
        try (CloseableHttpResponse response = execute(post)) {
            assertSetCookieCount(response, 9);
            assertCookie(response, CookieType.AUTH_SESSION_ID, "my-auth-session-id", "/realms/master/", -1, false, true, "Lax", true);
            assertCookie(response, CookieType.AUTH_SESSION_ID_HASH, "my-kc-auth-session", "/realms/master/", 60, false, false, "Lax", true);
            assertCookie(response, CookieType.AUTH_RESTART, "my-auth-restart", "/realms/master/", -1, false, true, "Lax", false);
            assertCookie(response, CookieType.AUTH_DETACHED, "my-auth-detached", "/realms/master/", 222, false, true, "Strict", false);
            assertCookie(response, CookieType.IDENTITY, "my-identity", "/realms/master/", 333, false, true, "Lax", true);
            assertCookie(response, CookieType.LOCALE, "my-locale", "/realms/master/", -1, false, true, "Lax", false);
            assertCookie(response, CookieType.LOGIN_HINT, "my-username", "/realms/master/", 31536000, false, true, "Lax", false);
            assertCookie(response, CookieType.SESSION, "my-session", "/realms/master/", 444, false, false, "Lax", true);
            assertCookie(response, CookieType.WELCOME_CSRF, "my-welcome-csrf", "/realms/master/cookie-testing/set-all", 300, false, true, "Strict", false);
        }
    }

    private CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());
        return httpClient.execute(request, context);
    }

    private URI cookieTestingUri(String subPath) {
        return KeycloakUriBuilder.fromUri(keycloakUrls.getMasterRealm())
                .path("cookie-testing/" + subPath)
                .build();
    }

    private URI insecureCookieTestingUri() {
        return KeycloakUriBuilder.fromUri(keycloakUrls.getMasterRealm().replace("localhost", "localtest.me"))
                .path("cookie-testing/set-all")
                .build();
    }
}
