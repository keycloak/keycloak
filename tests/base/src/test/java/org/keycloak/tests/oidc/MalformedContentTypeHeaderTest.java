package org.keycloak.tests.oidc;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
class MalformedContentTypeHeaderTest {

    private static final String MALFORMED_CONTENT_TYPE = "application/jso@n";

    @InjectHttpClient
    HttpClient httpClient;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    void malformedContentTypeOnUserInfoReturnsBadRequest() throws Exception {
        HttpGet get = new HttpGet(oauth.getEndpoints().getUserInfo());
        get.setHeader("Content-Type", MALFORMED_CONTENT_TYPE);

        HttpResponse response = httpClient.execute(get);
        try {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
        } finally {
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
        }
    }

    @Test
    void malformedContentTypeOnWellKnownReturnsBadRequest() throws Exception {
        HttpGet get = new HttpGet(oauth.getEndpoints().getOpenIDConfiguration());
        get.setHeader("Content-Type", MALFORMED_CONTENT_TYPE);

        HttpResponse response = httpClient.execute(get);
        try {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
        } finally {
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
        }
    }

    @Test
    void blankContentTypeOnUserInfoReturnsBadRequest() throws Exception {
        HttpGet get = new HttpGet(oauth.getEndpoints().getUserInfo());
        get.setHeader("Content-Type", "   ");

        HttpResponse response = httpClient.execute(get);
        try {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
        } finally {
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
        }
    }

    @Test
    void validContentTypeOnUserInfoIsNotRejected() throws Exception {
        HttpGet get = new HttpGet(oauth.getEndpoints().getUserInfo());
        get.setHeader("Content-Type", "application/json");

        HttpResponse response = httpClient.execute(get);
        try {
            assertThat(response.getStatusLine().getStatusCode(), is(401));
        } finally {
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
        }
    }
}
