package org.keycloak.tests.error;

import java.io.IOException;
import java.net.URI;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.keycloak.utils.MediaType.APPLICATION_JSON;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
class UncaughtErrorPageTest {

    @InjectPage
    ErrorPage errorPage;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectRealm(attachTo = "master")
    ManagedRealm masterRealm;

    @Test
    void invalidResource() {
        checkPageNotFound("/nosuch");
    }

    @Test
    void invalidRealm() {
        checkPageNotFound("/realms/nosuch");
    }

    @Test
    void invalidRealmResource() {
        checkPageNotFound("/realms/master/nosuch");
    }

    @Test
    void uncaughtErrorJson() throws IOException {
        HttpResponse response = httpClient.execute(new HttpGet(errorTestingUri("uncaught-error")));

        assertThat("status code", response.getStatusLine().getStatusCode(), is(500));

        OAuth2ErrorRepresentation error = readError(response);
        assertThat("error code", error.getError(), is("unknown_error"));
        assertThat("error description", error.getErrorDescription(),
                containsString("For more on this error consult the server log"));
    }

    @Test
    void uncaughtErrorClientRegistration() throws IOException {
        HttpPost post = new HttpPost(masterRealmUri("clients-registrations/openid-connect"));
        post.setEntity(new StringEntity("{ invalid : invalid }"));
        post.setHeader("Content-Type", APPLICATION_JSON);

        HttpResponse response = httpClient.execute(post);
        assertThat("status code", response.getStatusLine().getStatusCode(), is(400));

        OAuth2ErrorRepresentation error = readError(response);
        assertThat("error code", error.getError(), is(OAuthErrorException.INVALID_REQUEST));
        assertThat("error description", error.getErrorDescription(), is(notNullValue()));
    }

    @Test
    void uncaughtErrorAdmin() throws IOException {
        HttpPost post = newAdminPost(adminUri("realms/master/components"));
        post.setEntity(new StringEntity("{ invalid : invalid }"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code", response.getStatusLine().getStatusCode(), is(400));

        OAuth2ErrorRepresentation error = readError(response);
        assertThat("error code", error.getError(), is(OAuthErrorException.INVALID_REQUEST));
        assertThat("error description", error.getErrorDescription(), is(notNullValue()));
    }

    @Test
    void uncaughtErrorAdminPropertyError() throws IOException {
        HttpPost post = newAdminPost(adminUri("realms/master/components"));
        post.setEntity(new StringEntity("{\"<img src=alert(1)>\":1}"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code", response.getStatusLine().getStatusCode(), is(400));

        assertContentTypeJson(response);

        String body = EntityUtils.toString(response.getEntity());
        assertThat("XSS payload should be escaped in response", body,
                containsString("Unrecognized field \\\"<img src=alert(1)>\\\""));
    }

    @Test
    void uncaughtError() {
        driver.open(errorTestingUri("uncaught-error").toString());
        errorPage.assertCurrent();
        assertThat("error page message", errorPage.getError(), is("An internal server error has occurred"));
    }

    @Test
    void uncaughtErrorHeaders() throws IOException {
        HttpGet get = new HttpGet(errorTestingUri("uncaught-error"));
        get.setHeader("Accept", MediaType.TEXT_HTML_UTF_8);

        HttpResponse response = httpClient.execute(get);

        for (BrowserSecurityHeaders header : BrowserSecurityHeaders.values()) {
            String expectedValue = header.getDefaultValue();
            if (expectedValue == null || expectedValue.isEmpty()) {
                assertThat(header.getHeaderName() + " should be absent",
                        response.getFirstHeader(header.getHeaderName()), is(nullValue()));
            } else {
                Header actual = response.getFirstHeader(header.getHeaderName());
                assertThat(header.getHeaderName() + " should be present", actual, is(notNullValue()));
                assertThat(header.getHeaderName() + " value", actual.getValue(), is(expectedValue));
            }
        }
    }

    @Test
    void errorPageException() {
        oauth.realm("master");
        oauth.client("nosuch");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertThat("error page message", errorPage.getError(), is("Client not found."));
    }

    @Test
    void internationalisationEnabled() {
        masterRealm.updateWithCleanup(r -> r
                .internationalizationEnabled(true)
                .defaultLocale("en")
                .supportedLocales("en"));

        checkPageNotFound("/realms/master/nosuch");
        checkPageNotFound("/nosuch");
    }

    @Test
    void switchLocale() {
        masterRealm.updateWithCleanup(r -> r
                .internationalizationEnabled(true)
                .defaultLocale("en")
                .supportedLocales("en", "de"));

        checkPageNotFound("/realms/master/nosuch");
        String localeUrl = driver.findElement(
                By.xpath("//option[text()[contains(.,'Deutsch')]]")).getDomAttribute("value");
        driver.open(localeUrl);
        errorPage.assertCurrent();
    }

    @Test
    void jsonProcessingException() throws IOException {
        HttpPost post = newAdminPost(adminUri("realms/master/users"));
        post.setEntity(new StringEntity("[]"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code", response.getStatusLine().getStatusCode(), is(400));

        assertContentTypeJson(response);

        OAuth2ErrorRepresentation error = readError(response);
        assertThat("error code", error.getError(), is("unknown_error"));
    }

    private void checkPageNotFound(String path) {
        URI uri = keycloakUrls.getBaseBuilder().path(path).build();
        driver.open(uri.toString());
        errorPage.assertCurrent();
        assertThat("error message for path " + path, errorPage.getError(), is("Page not found"));
    }

    private HttpPost newAdminPost(URI uri) {
        HttpPost post = new HttpPost(uri);
        post.setHeader("Authorization", "Bearer " + adminClient.tokenManager().getAccessTokenString());
        post.setHeader("Content-Type", APPLICATION_JSON);
        return post;
    }

    private void assertContentTypeJson(HttpResponse response) {
        Header contentType = response.getFirstHeader("Content-Type");
        assertThat("Content-Type header", contentType, is(notNullValue()));
        assertThat("Content-Type value", contentType.getValue(), is(MediaType.APPLICATION_JSON));
    }

    private OAuth2ErrorRepresentation readError(HttpResponse response) throws IOException {
        return JsonSerialization.readValue(response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
    }

    private URI errorTestingUri(String subPath) {
        return KeycloakUriBuilder.fromUri(keycloakUrls.getMasterRealm())
                .path("error-testing/" + subPath)
                .build();
    }

    private URI masterRealmUri(String subPath) {
        return KeycloakUriBuilder.fromUri(keycloakUrls.getMasterRealm())
                .path(subPath)
                .build();
    }

    private URI adminUri(String subPath) {
        return keycloakUrls.getAdminBuilder().path(subPath).build();
    }
}
