package org.keycloak.testsuite.openshift;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.openshift.OpenShiftTokenReviewRequestRepresentation;
import org.keycloak.protocol.openshift.OpenShiftTokenReviewResponseRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.RestartContainer;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.keycloak.common.Profile.Feature.OPENSHIFT_INTEGRATION;
import static org.keycloak.testsuite.ProfileAssume.assumeFeatureEnabled;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = OPENSHIFT_INTEGRATION, skipRestart = true)
public class OpenShiftTokenReviewEndpointTest extends AbstractTestRealmKeycloakTest {

    private static boolean flowConfigured;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ClientRepresentation client = testRealm.getClients().stream().filter(r -> r.getClientId().equals("test-app")).findFirst().get();

        List<ProtocolMapperRepresentation> mappers = new LinkedList<>();
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("groups");
        mapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put("full.path", "false");
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        mappers.add(mapper);

        client.setProtocolMappers(mappers);
        client.setPublicClient(false);
        client.setClientAuthenticatorType("testsuite-client-dummy");

        testRealm.getUsers().add(UserBuilder.create().username("groups-user").password("password").addGroups("/topGroup", "/topGroup/level2group").build());
    }

    @Before
    public void enablePassthroughAuthenticator() {
        if (!flowConfigured) {
            HashMap<String, String> data = new HashMap<>();
            data.put("newName", "testsuite-client-dummy");
            Response response = testRealm().flows().copy("clients", data);
            assertEquals(201, response.getStatus());
            response.close();

            data = new HashMap<>();
            data.put("provider", "testsuite-client-dummy");
            data.put("requirement", "ALTERNATIVE");

            testRealm().flows().addExecution("testsuite-client-dummy", data);

            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setClientAuthenticationFlow("testsuite-client-dummy");
            testRealm().update(realmRep);

            List<AuthenticationExecutionInfoRepresentation> executions = testRealm().flows().getExecutions("testsuite-client-dummy");
            for (AuthenticationExecutionInfoRepresentation e : executions) {
                if (e.getProviderId().equals("testsuite-client-dummy")) {
                    e.setRequirement("ALTERNATIVE");
                    testRealm().flows().updateExecutions("testsuite-client-dummy", e);
                }
            }
            flowConfigured = true;
        }
    }

    @Test
    public void basicTest() {
        Review r = new Review().invoke();

        String userId = testRealm().users().search(r.username).get(0).getId();

        OpenShiftTokenReviewResponseRepresentation.User user = r.response.getStatus().getUser();

        assertEquals(userId, user.getUid());
        assertEquals("test-user@localhost", user.getUsername());
        assertNotNull(user.getExtra());

        r.assertScope("openid", "email", "profile");
    }

    @Test
    public void longExpiration() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = client.toRepresentation();

        try {
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN, "-1");
            client.update(clientRep);

            // Set time offset just before SSO idle, to get session last refresh updated

            setTimeOffset(1500);

            Review review = new Review();

            review.invoke().assertSuccess();

            // Bump last refresh updated again

            setTimeOffset(3000);

            review.invoke().assertSuccess();

            // And, again

            setTimeOffset(4500);

            // Token should still be valid as session last refresh should have been updated

            review.invoke().assertSuccess();
        } finally {
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN, null);
            client.update(clientRep);
        }
    }

    @Test
    public void hs256() {
        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();

        try {
            rep.setDefaultSignatureAlgorithm(Algorithm.HS256);
            realm.update(rep);

            Review r = new Review().algorithm(Algorithm.HS256).invoke()
                    .assertSuccess();

            String userId = testRealm().users().search(r.username).get(0).getId();

            OpenShiftTokenReviewResponseRepresentation.User user = r.response.getStatus().getUser();

            assertEquals(userId, user.getUid());
            assertEquals("test-user@localhost", user.getUsername());
            assertNotNull(user.getExtra());

            r.assertScope("openid", "email", "profile");
        } finally {
            rep.setDefaultSignatureAlgorithm(null);
            realm.update(rep);
        }
    }

    @Test
    public void groups() {
        new Review().username("groups-user")
                .invoke()
                .assertSuccess().assertGroups("topGroup", "level2group");
    }

    @Test
    public void customScopes() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setProtocol("openid-connect");
        clientScope.setId("user:info");
        clientScope.setName("user:info");

        testRealm().clientScopes().create(clientScope);

        ClientRepresentation clientRep = testRealm().clients().findByClientId("test-app").get(0);

        testRealm().clients().get(clientRep.getId()).addOptionalClientScope("user:info");

        try {
            oauth.scope("user:info");
            new Review()
                    .invoke()
                    .assertSuccess().assertScope("openid", "user:info", "profile", "email");
        } finally {
            testRealm().clients().get(clientRep.getId()).removeOptionalClientScope("user:info");
        }
    }

    @Test
    public void emptyScope() throws Exception {
        ClientRepresentation clientRep = testRealm().clients().findByClientId("test-app").get(0);
        List<ClientScopeRepresentation> scopesBefore = testRealm().clients().get(clientRep.getId()).getDefaultClientScopes();

        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, "test", clientRep.getClientId())
                .setConsentRequired(false)
                .setFullScopeAllowed(false)
                .setDefaultClientScopes(Collections.EMPTY_LIST)
                .update()) {

            oauth.openid(false);
            try {
                new Review()
                        .invoke()
                        .assertSuccess()
                        .assertEmptyScope();
            } finally {
                oauth.openid(true);
            }
        }
        // The default client scopes should be same like before.
        int scopesAfterSize = testRealm().clients().get(clientRep.getId()).getDefaultClientScopes().size();
        assertEquals(scopesBefore.size(), scopesAfterSize);
    }

    @Test
    public void expiredToken() {
        try {
            new Review()
                    .runAfterTokenRequest(i -> setTimeOffset(testRealm().toRepresentation().getAccessTokenLifespan() + 10))
                    .invoke()
                    .assertError(401, "Token verification failure");
        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void invalidPublicKey() {
        new Review()
                .runAfterTokenRequest(i -> {
                    String header = i.token.split("\\.")[0];
                    String s = new String(Base64Url.decode(header));
                    s = s.replace(",\"kid\" : \"", ",\"kid\" : \"x");
                    String newHeader = Base64Url.encode(s.getBytes());
                    i.token = i.token.replaceFirst(header, newHeader);
                })
                .invoke()
                .assertError(401, "Token verification failure");
    }

    @Test
    public void noUserSession() {
        new Review()
                .runAfterTokenRequest(i -> {
                    String userId = testRealm().users().search(i.username).get(0).getId();
                    testRealm().users().get(userId).logout();
                })
                .invoke()
                .assertError(401, "Token verification failure");
    }

    @Test
    public void invalidTokenSignature() {
        new Review()
                .runAfterTokenRequest(i -> i.token += "x")
                .invoke()
                .assertError(401, "Token verification failure");
    }

    @Test
    public void realmDisabled() {
        RealmRepresentation r = testRealm().toRepresentation();
        try {
            new Review().runAfterTokenRequest(i -> {
                r.setEnabled(false);
                testRealm().update(r);
            }).invoke().assertError(401, null);


        } finally {
            r.setEnabled(true);
            testRealm().update(r);
        }
    }

    @Test
    public void publicClientNotPermitted() {
        ClientRepresentation clientRep = testRealm().clients().findByClientId("test-app").get(0);
        clientRep.setPublicClient(true);
        testRealm().clients().get(clientRep.getId()).update(clientRep);
        try {
            new Review().invoke().assertError(401, "Public client is not permitted to invoke token review endpoint");
        } finally {
            clientRep.setPublicClient(false);
            testRealm().clients().get(clientRep.getId()).update(clientRep);
        }
    }

    private class Review {

        private String realm = "test";
        private String clientId = "test-app";
        private String username = "test-user@localhost";
        private String password = "password";
        private String algorithm = Algorithm.RS256;
        private InvokeRunnable runAfterTokenRequest;

        private String token;
        private int responseStatus;
        private OpenShiftTokenReviewResponseRepresentation response;

        public Review username(String username) {
            this.username = username;
            return this;
        }

        public Review algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Review runAfterTokenRequest(InvokeRunnable runnable) {
            this.runAfterTokenRequest = runnable;
            return this;
        }

        public Review invoke() {
            try {
                if (token == null) {
                    String userId = testRealm().users().search(username).get(0).getId();
                    oauth.doLogin(username, password);
                    EventRepresentation loginEvent = events.expectLogin().user(userId).assertEvent();

                    String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
                    OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");

                    events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId()).detail("client_auth_method", "testsuite-client-dummy").user(userId).assertEvent();

                    token = accessTokenResponse.getAccessToken();
                }

                assertEquals(algorithm, new JWSInput(token).getHeader().getAlgorithm().name());

                if (runAfterTokenRequest != null) {
                    runAfterTokenRequest.run(this);
                }

                try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                    String url = AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/realms/" + realm + "/protocol/openid-connect/ext/openshift-token-review/" + clientId;

                    OpenShiftTokenReviewRequestRepresentation request = new OpenShiftTokenReviewRequestRepresentation();
                    OpenShiftTokenReviewRequestRepresentation.Spec spec = new OpenShiftTokenReviewRequestRepresentation.Spec();
                    spec.setToken(token);
                    request.setSpec(spec);

                    HttpPost post = new HttpPost(url);
                    post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
                    post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
                    post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(request)));

                    try (CloseableHttpResponse resp = client.execute(post)) {
                        responseStatus = resp.getStatusLine().getStatusCode();
                        response = JsonSerialization.readValue(resp.getEntity().getContent(), OpenShiftTokenReviewResponseRepresentation.class);
                    }

                    assertEquals("authentication.k8s.io/v1beta1", response.getApiVersion());
                    assertEquals("TokenReview", response.getKind());
                }
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Review assertSuccess() {
            assertEquals(200, responseStatus);
            assertTrue(response.getStatus().isAuthenticated());
            assertNotNull(response.getStatus().getUser());
            return this;
        }

        private Review assertError(int expectedStatus, String expectedReason) {
            assertEquals(expectedStatus, responseStatus);
            assertFalse(response.getStatus().isAuthenticated());
            assertNull(response.getStatus().getUser());

            if (expectedReason != null) {
                EventRepresentation poll = events.poll();
                assertEquals(expectedReason, poll.getDetails().get(Details.REASON));
            }

            return this;
        }

        private void assertScope(String... expectedScope) {
            List<String> actualScopes = Arrays.asList(response.getStatus().getUser().getExtra().getScopes());
            assertEquals(expectedScope.length, actualScopes.size());
            assertThat(actualScopes, containsInAnyOrder(expectedScope));
        }

        private void assertEmptyScope() {
            assertNull(response.getStatus().getUser().getExtra());
        }

        private void assertGroups(String... expectedGroups) {
            List<String> actualGroups = new LinkedList<>(response.getStatus().getUser().getGroups());
            assertEquals(expectedGroups.length, actualGroups.size());
            assertThat(actualGroups, containsInAnyOrder(expectedGroups));
        }

    }

    private interface InvokeRunnable {
        void run(Review i);
    }

}
