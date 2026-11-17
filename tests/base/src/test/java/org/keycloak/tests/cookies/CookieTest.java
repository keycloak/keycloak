package org.keycloak.tests.cookies;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.cookie.CookieType;
import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@KeycloakIntegrationTest
class CookieTest {

    @InjectRealm(config = CookieTestRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @Test
    void testCookieValue() throws Exception {
        assertCookieValueDoesNotAuthenticate(() -> {});
    }

    @Test
    void testCookieValueLoggedOut() throws Exception {
        assertCookieValueDoesNotAuthenticate(() -> AccountHelper.logout(realm.admin(), "test-user@localhost"));
    }

    @Test
    void testNoDuplicationsWhenExpiringCookies() throws IOException {
        assertThat("Server must be running with TLS", keycloakUrls.getBase(), startsWith("https"));

        oauth.doLogin("test-user@localhost", "password");
        assertThat("Login should succeed", oauth.parseLoginResponse().isSuccess(), is(true));

        driver.driver().navigate().to(keycloakUrls.getBase() + "/realms/" + realm.getName() + "/login-actions/authenticate/");

        Cookie invalidIdentityCookie = driver.driver().manage().getCookieNamed(CookieType.IDENTITY.getName());
        assertThat(invalidIdentityCookie, notNullValue());

        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie clientCookie = new BasicClientCookie(invalidIdentityCookie.getName(), invalidIdentityCookie.getValue());
        clientCookie.setDomain(invalidIdentityCookie.getDomain());
        clientCookie.setPath(invalidIdentityCookie.getPath());
        cookieStore.addCookie(clientCookie);

        String authUrl = keycloakUrls.getBase() + "/realms/" + realm.getName()
                + "/protocol/openid-connect/auth?response_type=code&client_id=" + Constants.ACCOUNT_CONSOLE_CLIENT_ID
                + "&redirect_uri=" + keycloakUrls.getBase() + "/realms/" + realm.getName() + "/account&scope=openid";

        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).setSSLContext(managedCertificates.getClientSSLContext()).build()) {
            HttpGet get = new HttpGet(authUrl);
            try (CloseableHttpResponse response = client.execute(get)) {
            Header[] headers = response.getHeaders(HttpHeaders.SET_COOKIE);
            Set<String> cookies = new HashSet<>();

            for (Header header : headers) {
                assertThat("Cookie '" + header.getValue() + "' is duplicated", cookies.add(header.getValue()), is(true));
            }

            assertThat("Set-Cookie headers should not be empty", cookies, is(not(empty())));

            }
        }
    }

    private void assertCookieValueDoesNotAuthenticate(Runnable afterLogin) throws Exception {
        AuthorizationEndpointResponse codeResponse = oauth.doLogin("test-user@localhost", "password");
        AccessTokenResponse accTokenResp = oauth.doAccessTokenRequest(codeResponse.getCode());
        assertThat("Login should succeed", oauth.parseLoginResponse().isSuccess(), is(true));

        afterLogin.run();

        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie(CookieType.IDENTITY.getName(), accTokenResp.getAccessToken());
        cookie.setDomain("localhost");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        String loginFormUrl = oauth.loginForm().build();
        HttpGet get = new HttpGet(loginFormUrl);
        try (CloseableHttpResponse resp = httpClient.execute(get, localContext)) {
            String pageContent = EntityUtils.toString(resp.getEntity());

            assertThat(pageContent, not(containsString("First name")));
            assertThat(pageContent, not(containsString("Last name")));

            assertThat(pageContent, containsString("Sign In"));
            assertThat(pageContent, containsString("Forgot Password?"));
        }
    }

    static class TlsEnabledConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }

    static class CookieTestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("test")
                    .resetPasswordAllowed(true)
                    .users(UserBuilder.create("test-user@localhost")
                            .password("password")
                            .email("test-user@localhost")
                            .name("Test", "User")
                            .emailVerified(true));
        }
    }
}
