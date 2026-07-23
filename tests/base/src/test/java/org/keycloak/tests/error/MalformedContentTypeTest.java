package org.keycloak.tests.error;

import java.io.IOException;
import java.net.URI;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
class MalformedContentTypeTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectRealm(attachTo = "master")
    ManagedRealm masterRealm;

    @Test
    void malformedContentTypeOnTokenEndpoint() throws IOException {
        HttpPost post = new HttpPost(tokenUri());
        post.setHeader("Content-Type", "invalid/@@##");
        post.setEntity(new StringEntity("grant_type=password&client_id=admin-cli&username=admin&password=admin"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code should be 400", response.getStatusLine().getStatusCode(), is(400));

        OAuth2ErrorRepresentation error = JsonSerialization.readValue(
                response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
        assertThat("error code", error.getError(), is(OAuthErrorException.INVALID_REQUEST));
    }

    @Test
    void emptyContentTypeOnTokenEndpoint() throws IOException {
        HttpPost post = new HttpPost(tokenUri());
        post.setHeader("Content-Type", "");
        post.setEntity(new StringEntity("grant_type=password&client_id=admin-cli&username=admin&password=admin"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code should be 400", response.getStatusLine().getStatusCode(), is(400));

        OAuth2ErrorRepresentation error = JsonSerialization.readValue(
                response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
        assertThat("error code", error.getError(), is(OAuthErrorException.INVALID_REQUEST));
    }

    @Test
    void xssPayloadContentTypeOnTokenEndpoint() throws IOException {
        HttpPost post = new HttpPost(tokenUri());
        post.setHeader("Content-Type", "</>\"alert(1)");
        post.setEntity(new StringEntity("grant_type=password&client_id=admin-cli&username=admin&password=admin"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code should be 400", response.getStatusLine().getStatusCode(), is(400));

        OAuth2ErrorRepresentation error = JsonSerialization.readValue(
                response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
        assertThat("error code", error.getError(), is(OAuthErrorException.INVALID_REQUEST));
    }

    @Test
    void xssScriptPayloadContentTypeOnTokenEndpoint() throws IOException {
        HttpPost post = new HttpPost(tokenUri());
        post.setHeader("Content-Type", "</><script>alert(1)</script>");
        post.setEntity(new StringEntity("grant_type=password&client_id=admin-cli&username=admin&password=admin"));

        HttpResponse response = httpClient.execute(post);
        assertThat("status code should be 400", response.getStatusLine().getStatusCode(), is(400));

        OAuth2ErrorRepresentation error = JsonSerialization.readValue(
                response.getEntity().getContent(), OAuth2ErrorRepresentation.class);
        assertThat("error code", error.getError(), is(OAuthErrorException.INVALID_REQUEST));
    }

    private URI tokenUri() {
        return KeycloakUriBuilder.fromUri(keycloakUrls.getMasterRealm())
                .path("protocol/openid-connect/token")
                .build();
    }
}
