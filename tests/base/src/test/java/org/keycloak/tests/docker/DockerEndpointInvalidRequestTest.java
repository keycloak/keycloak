package org.keycloak.tests.docker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = DockerEndpointInvalidRequestTest.DockerServerConfig.class)
public class DockerEndpointInvalidRequestTest {

    private static final String REALM_ID = "docker-invalid-request-realm";
    private static final String CLIENT_ID = "docker-test-client";
    private static final String DOCKER_USER = "docker-user";
    private static final String DOCKER_USER_PASSWORD = "password";

    @InjectRealm(config = DockerRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectEvents
    Events events;

    @Test
    public void shouldRejectDuplicatedParameter() throws IOException {
        // 'state' is sent twice, so the request cannot be parsed unambiguously
        try (CloseableHttpResponse response = get(authUrl() + "&scope=repository:foo:pull&state=one&state=two")) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            String body = EntityUtils.toString(response.getEntity());
            assertThat(body, containsString("invalid_request"));
            assertThat(body, containsString("duplicated parameter"));
        }

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .clientId(CLIENT_ID)
                .error(Errors.INVALID_REQUEST)
                .details(Details.REASON, "duplicated parameter");
    }

    @Test
    public void shouldNotRejectWellFormedRequest() throws IOException {
        // The same parameter sent only once must still authenticate and return a docker token
        try (CloseableHttpResponse response = get(authUrl() + "&scope=repository:foo:pull")) {
            assertThat("A well formed request must not be rejected as malformed",
                    response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("token"));
        }

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .clientId(CLIENT_ID)
                .details(Details.AUTH_METHOD, DockerAuthV2Protocol.LOGIN_PROTOCOL);
    }

    private CloseableHttpResponse get(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        String credentials = DOCKER_USER + ":" + DOCKER_USER_PASSWORD;
        get.setHeader(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        return httpClient.execute(get);
    }

    private String authUrl() {
        return managedRealm.getBaseUrl() + "/protocol/docker-v2/auth"
                + "?service=" + CLIENT_ID
                + "&account=" + DOCKER_USER
                + "&client_id=docker"
                + "&offline_token=true";
    }

    public static class DockerServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.DOCKER);
        }
    }

    public static class DockerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realmBuilder) {
            RealmRepresentation dockerRealm = DockerTestRealmSetup.createRealm(REALM_ID);
            DockerTestRealmSetup.configureDockerRegistryClient(dockerRealm, CLIENT_ID);
            DockerTestRealmSetup.configureUser(dockerRealm, DOCKER_USER, DOCKER_USER_PASSWORD);
            return RealmBuilder.update(dockerRealm);
        }
    }
}
