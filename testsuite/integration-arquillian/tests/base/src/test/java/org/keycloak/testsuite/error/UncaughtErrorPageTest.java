package org.keycloak.testsuite.error;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;
import org.openqa.selenium.By;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.utils.MediaType.APPLICATION_JSON;

public class UncaughtErrorPageTest extends AbstractKeycloakTest {

    @Page
    private ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void invalidResource() throws MalformedURLException {
        checkPageNotFound("/auth/nosuch");
    }

    @Test
    public void invalidRealm() throws MalformedURLException {
        checkPageNotFound("/auth/realms/nosuch");
    }

    @Test
    public void invalidRealmResource() throws MalformedURLException {
        checkPageNotFound("/auth/realms/master/nosuch");
    }

    @Test
    @UncaughtServerErrorExpected
    public void uncaughtErrorJson() throws IOException {
        Response response = testingClient.testing().uncaughtError();
        assertEquals(500, response.getStatus());

        InputStream is = (InputStream) response.getEntity();
        String responseString = StreamUtil.readString(is, StandardCharsets.UTF_8);

        Assert.assertTrue(responseString.contains("An internal server error has occurred"));
    }

    @Test
    @UncaughtServerErrorExpected
    public void uncaughtErrorClientRegistration() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/realms/master/clients-registrations/openid-connect").build());
            post.setEntity(new StringEntity("{ invalid : invalid }"));
            post.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(400, response.getStatusLine().getStatusCode());

                OAuth2ErrorRepresentation error = JsonSerialization.readValue(response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
                assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
                assertNotNull("found error with " + error.getError() + "/" + error.getErrorDescription(), error.getErrorDescription());
            }
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void uncaughtErrorAdmin() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String accessToken = adminClient.tokenManager().getAccessTokenString();

            HttpPost post = new HttpPost(suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/admin/realms/master/components").build());
            post.setEntity(new StringEntity("{ invalid : invalid }"));
            post.setHeader("Authorization", "bearer " + accessToken);
            post.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(400, response.getStatusLine().getStatusCode());

                OAuth2ErrorRepresentation error = JsonSerialization.readValue(response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
                assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
                assertNotNull("found error with " + error.getError() + "/" + error.getErrorDescription(), error.getErrorDescription());
            }
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void uncaughtErrorAdminPropertyError() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String accessToken = adminClient.tokenManager().getAccessTokenString();

            HttpPost post = new HttpPost(suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/admin/realms/master/components").build());
            post.setEntity(new StringEntity("{\"<img src=alert(1)>\":1}"));
            post.setHeader("Authorization", "bearer " + accessToken);
            post.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(400, response.getStatusLine().getStatusCode());

                Header header = response.getFirstHeader("Content-Type");
                assertThat(header, notNullValue());

                // Verify the Content-Type is not text/html
                assertThat(Arrays.stream(header.getElements())
                        .map(HeaderElement::getName)
                        .filter(Objects::nonNull)
                        .anyMatch(f -> f.equals(APPLICATION_JSON)), is(true));

                // The alert is not executed
                assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("Unrecognized field \\\"<img src=alert(1)>\\\""));
            }
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void uncaughtError() throws MalformedURLException {
        URI uri = suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/realms/master/testing/uncaught-error").build();
        driver.navigate().to(uri.toURL());

        assertTrue(errorPage.isCurrent());
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    @Test
    @UncaughtServerErrorExpected
    public void uncaughtErrorHeaders() throws IOException {
        URI uri = suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/realms/master/testing/uncaught-error").build();

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            SimpleHttpResponse response = SimpleHttpDefault.doGet(uri.toString(), client).header("Accept", MediaType.TEXT_HTML_UTF_8).asResponse();

            for (BrowserSecurityHeaders header : BrowserSecurityHeaders.values()) {
                String expectedValue = header.getDefaultValue();

                if (expectedValue == null || expectedValue.isEmpty()) {
                    assertNull(response.getFirstHeader(header.getHeaderName()));
                } else {
                    assertEquals(expectedValue, response.getFirstHeader(header.getHeaderName()));
                }
            }
        }
    }

    @Test
    public void errorPageException() {
        oauth.realm("master");
        oauth.clientId("nosuch");
        oauth.openLoginForm();

        assertTrue(errorPage.isCurrent());
        assertEquals("Client not found.", errorPage.getError());
    }

    @Test
    public void internationalisationEnabled() throws MalformedURLException {
        RealmResource testRealm = realmsResouce().realm("master");
        RealmRepresentation rep = testRealm.toRepresentation();
        rep.setInternationalizationEnabled(true);
        rep.setDefaultLocale("en");
        rep.setSupportedLocales(Collections.singleton("en"));
        testRealm.update(rep);

        try {
            checkPageNotFound("/auth/realms/master/nosuch");
            checkPageNotFound("/auth/nosuch");
        } finally {
            rep.setInternationalizationEnabled(false);
            testRealm.update(rep);
        }
    }

    @Test
    public void switchLocale() throws MalformedURLException {
        RealmResource testRealm = realmsResouce().realm("master");
        RealmRepresentation rep = testRealm.toRepresentation();
        rep.setInternationalizationEnabled(true);
        rep.setDefaultLocale("en");
        HashSet<String> supported = new HashSet<>();
        supported.add("en");
        supported.add("de");
        rep.setSupportedLocales(supported);
        testRealm.update(rep);

        try {
            checkPageNotFound("/auth/realms/master/nosuch");
             String url = driver.findElement(By.xpath("//option[text()[contains(.,'Deutsch')]]")).getAttribute("value");
            driver.navigate().to(url);
            errorPage.assertCurrent();
        } finally {
            rep.setInternationalizationEnabled(false);
            testRealm.update(rep);
        }
    }

    private void checkPageNotFound(String path) throws MalformedURLException {
        URI uri = suiteContext.getAuthServerInfo().getUriBuilder().path(path).build();
        driver.navigate().to(uri.toURL());

        assertTrue(errorPage.isCurrent());
        assertEquals("Page not found", errorPage.getError());
    }

    @Test
    public void jsonProcessingException() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String accessToken = adminClient.tokenManager().getAccessTokenString();

            // send an empty array to the user endpoint which expects a User json object
            HttpPost post = new HttpPost(suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/admin/realms/master/users").build());
            post.setEntity(new StringEntity("[]"));
            post.setHeader("Authorization", "bearer " + accessToken);
            post.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());

                Header header = response.getFirstHeader("Content-Type");
                assertThat(header, notNullValue());
                assertEquals(MediaType.APPLICATION_JSON, header.getValue());

                OAuth2ErrorRepresentation error = JsonSerialization.readValue(response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
                assertEquals("unknown_error", error.getError());
            }
        }
    }
}
