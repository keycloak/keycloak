package org.keycloak.tests.error;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
class JacksonErrorResponseTest {

    @InjectKeycloakUrls
    KeycloakUrls urls;

    @InjectHttpClient
    HttpClient client;

    @Test
    void validInputCommits() throws IOException {
        String transaction = UUID.randomUUID().toString();
        HttpResponse response = post("", "{\"count\":7}", "application/json", transaction);
        String body = EntityUtils.toString(response.getEntity());
        assertThat(body, response.getStatusLine().getStatusCode(), is(200));
        assertThat(JsonSerialization.mapper.readTree(body).get("count").asInt(), is(7));
        assertTransaction(transaction, "committed");
    }

    @Test
    void typeMismatchIsStructuredBadRequest() throws IOException {
        String transaction = UUID.randomUUID().toString();
        assertJsonError(post("", "{\"count\":{}}", "application/json", transaction), 400,
                "unknown_error", "Cannot parse the JSON");
        assertTransaction(transaction, "rolled-back");
    }

    @Test
    void streamConstraintIsStructuredBadRequest() throws IOException {
        // Exceeds Jackson's default maximum number length, not merely the target int range.
        assertJsonError(post("", "{\"count\":" + "1".repeat(1001) + "}", "application/json", null), 400,
                "unknown_error", "Cannot parse the JSON");
    }

    @Test
    void throwingResponseGetterIsServerError() throws IOException {
        HttpGet get = new HttpGet(uri("/throwing-getter", null));
        get.setHeader("Accept", "application/json");
        assertJsonError(client.execute(get), 500, "unknown_error", "For more on this error consult the server log.");
    }

    @Test
    void invalidResponseDefinitionIsServerError() throws IOException {
        HttpResponse response = client.execute(new HttpGet(uri("/invalid-definition", null)));
        EntityUtils.consume(response.getEntity());
        assertThat(response.getStatusLine().getStatusCode(), is(500));
    }

    @Test
    void invalidRequestDefinitionIsServerError() throws IOException {
        HttpResponse response = post("/invalid-definition", "{}", "application/json", null);
        EntityUtils.consume(response.getEntity());
        assertThat(response.getStatusLine().getStatusCode(), is(500));
    }

    @Test
    void malformedInputNegotiatesHtml() throws IOException {
        assertHtmlError(post("", "{\"count\":", "text/html, application/json;q=0.9", null), 400);
    }

    @Test
    void responseFailureNegotiatesHtml() throws IOException {
        HttpGet get = new HttpGet(uri("/throwing-getter", null));
        get.setHeader("Accept", "text/html, application/json;q=0.9");
        assertHtmlError(client.execute(get), 500);
    }

    @Test
    void legacyMapperRemainsAvailableThroughCdi() throws IOException {
        HttpResponse response = client.execute(new HttpGet(uri("/legacy-mapper", null)));
        String json = EntityUtils.toString(response.getEntity());
        assertThat(json, response.getStatusLine().getStatusCode(), is(200));
        var body = JsonSerialization.mapper.readTree(json);
        assertThat(body.get("optional").asText(), is("value"));
        assertThat(body.get("date"), is(JsonSerialization.mapper.readTree("[2026,1,2]")));
    }

    private HttpResponse post(String path, String body, String accept, String transaction) throws IOException {
        HttpPost post = new HttpPost(uri(path, transaction));
        post.setHeader("Accept", accept);
        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        return client.execute(post);
    }

    private void assertJsonError(HttpResponse response, int status, String code, String description) throws IOException {
        String body = EntityUtils.toString(response.getEntity());
        assertThat(body, response.getStatusLine().getStatusCode(), is(status));
        assertThat(response.getFirstHeader("Content-Type").getValue(), containsString("application/json"));
        var error = JsonSerialization.mapper.readTree(body);
        assertThat(error.get("error").asText(), is(code));
        assertThat(error.get("error_description").asText(), is(description));
        assertThat(body, not(containsString("Private serialization failure detail")));
    }

    private void assertHtmlError(HttpResponse response, int status) throws IOException {
        String body = EntityUtils.toString(response.getEntity());
        assertThat(body, response.getStatusLine().getStatusCode(), is(status));
        assertThat(response.getFirstHeader("Content-Type").getValue(), containsString("text/html"));
        assertThat(body, containsString("An internal server error has occurred"));
        assertThat(body, not(containsString("Private serialization failure detail")));
    }

    private void assertTransaction(String transaction, String outcome) throws IOException {
        HttpResponse response = client.execute(new HttpGet(uri("/transaction", transaction)));
        String body = EntityUtils.toString(response.getEntity());
        assertThat(body, response.getStatusLine().getStatusCode(), is(200));
        assertThat(body, is(outcome));
    }

    private URI uri(String path, String transaction) {
        var builder = KeycloakUriBuilder.fromUri(urls.getMasterRealm()).path("error-testing/json" + path);
        if (transaction != null) {
            builder.queryParam("transaction", transaction);
        }
        return builder.build();
    }
}
