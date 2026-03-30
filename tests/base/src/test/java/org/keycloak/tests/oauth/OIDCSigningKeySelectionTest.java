package org.keycloak.tests.oauth;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for client-specific signing key selection in OIDC protocol.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
@KeycloakIntegrationTest
public class OIDCSigningKeySelectionTest {

    private static final String RSA_GENERATED_PROVIDER_ID = "rsa-generated";
    private static final String TEST_CLIENT = "test-app";
    private static final String TEST_CLIENT_SECRET = "test-secret";
    // Must be lower than DefaultKeyProviders.DEFAULT_PRIORITY ("100") so the test key
    // does not become the realm's active key.
    private static final String TEST_KEY_PRIORITY = "50";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    // METHOD lifecycle ensures each test starts with a fresh client, avoiding attribute leakage
    // between tests (Admin API update does not remove attributes that were added).
    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectClient(config = MultipleKeysOAuthClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient multipleKeysClient;

    @InjectHttpServer
    HttpServer httpServer;

    private RealmResource realmResource;
    private String newKeyKid;
    private String newKeyPublicKeyPem;
    private String realmActiveKeyKid;
    private String realmActiveKeyPublicKeyPem;

    @BeforeEach
    public void setupSigningKey() {
        realmResource = realm.admin();

        // Create key provider and register cleanup to remove it after each test
        String keyProviderId = createRs256KeyProvider(realmResource, "test-signing-key");
        realm.cleanup().add(r -> r.components().component(keyProviderId).remove());

        KeysMetadataRepresentation keysMetadata = realmResource.keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation newKeyMeta = findKeyByProviderId(keysMetadata, keyProviderId);
        assertNotNull(newKeyMeta, "New key should be created");
        newKeyKid = newKeyMeta.getKid();
        newKeyPublicKeyPem = newKeyMeta.getPublicKey();
        assertNotNull(newKeyPublicKeyPem, "Public key should be available");

        realmActiveKeyKid = keysMetadata.getActive().get(Algorithm.RS256);
        KeysMetadataRepresentation.KeyMetadataRepresentation realmActiveKeyMeta = findKeyByKid(keysMetadata, realmActiveKeyKid);
        realmActiveKeyPublicKeyPem = realmActiveKeyMeta.getPublicKey();

        // Verify preconditions: realm active key priority must be higher than test key priority
        assertEquals(100L, realmActiveKeyMeta.getProviderPriority(),
            "Realm active key priority should be 100 (DefaultKeyProviders.DEFAULT_PRIORITY)");
        assertNotEquals(realmActiveKeyKid, newKeyKid, "Test key must differ from realm active key");
    }

    @Test
    public void testAccessTokenSigningKey() throws Exception {
        // Without KID — should use realm active key
        AccessTokenResponse defaultResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertSignedWithKey(defaultResponse.getAccessToken(), realmActiveKeyKid, realmActiveKeyPublicKeyPem);

        // With KID — should use configured key
        configureTestApp(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, newKeyKid);

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertSignedWithKey(tokenResponse.getAccessToken(), newKeyKid, newKeyPublicKeyPem);
    }

    @Test
    public void testIdTokenAndLogoutTokenSigningKey() throws Exception {
        String backchannelLogoutPath = "/backchannel-logout";
        AtomicReference<String> logoutTokenRef = new AtomicReference<>();
        httpServer.createContext(backchannelLogoutPath, exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            logoutTokenRef.set(extractLogoutToken(body));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });

        String backchannelLogoutUrl = "http://127.0.0.1:8500" + backchannelLogoutPath;

        try {
            // Without KID — should use realm active key for both ID token and logout token
            configureTestApp(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, backchannelLogoutUrl);

            AccessTokenResponse defaultResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertSignedWithKey(defaultResponse.getIdToken(), realmActiveKeyKid, realmActiveKeyPublicKeyPem);

            // Verify logout token also uses realm active key
            oauth.doLogin("test-user@localhost", "password");
            AccessTokenResponse browserTokenResponse = oauth.accessTokenRequest(
                oauth.parseLoginResponse().getCode()).send();

            oauth.logoutForm().idTokenHint(browserTokenResponse.getIdToken()).open();

            String rawLogoutToken = logoutTokenRef.get();
            assertNotNull(rawLogoutToken, "Logout token should be received");
            assertSignedWithKey(rawLogoutToken, realmActiveKeyKid, realmActiveKeyPublicKeyPem);

            // With KID — should use configured key for ID token and logout token
            logoutTokenRef.set(null);
            configureTestApp(
                OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_KID, newKeyKid,
                OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, backchannelLogoutUrl);

            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertSignedWithKey(tokenResponse.getIdToken(), newKeyKid, newKeyPublicKeyPem);

            // Verify logout token uses the same key (LOGOUT category shares ID token key)
            oauth.doLogin("test-user@localhost", "password");
            browserTokenResponse = oauth.accessTokenRequest(
                oauth.parseLoginResponse().getCode()).send();

            oauth.logoutForm().idTokenHint(browserTokenResponse.getIdToken()).open();

            rawLogoutToken = logoutTokenRef.get();
            assertNotNull(rawLogoutToken, "Logout token should be received");
            assertSignedWithKey(rawLogoutToken, newKeyKid, newKeyPublicKeyPem);
        } finally {
            httpServer.removeContext(backchannelLogoutPath);
        }
    }

    @Test
    public void testUserInfoSigningKey() throws Exception {
        // Without KID — should use realm active key
        configureTestApp(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS256);
        assertUserInfoSignedWithKey(realmActiveKeyKid, realmActiveKeyPublicKeyPem);

        // With KID — should use configured key
        configureTestApp(
            OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS256,
            OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_KID, newKeyKid);
        assertUserInfoSignedWithKey(newKeyKid, newKeyPublicKeyPem);
    }

    private void assertUserInfoSignedWithKey(String expectedKid, String expectedPublicKeyPem) throws Exception {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        // UserInfo response is signed (JWT), so we need to read the raw response string
        HttpGet get = new HttpGet(oauth.getEndpoints().getUserInfo());
        get.setHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
        try (CloseableHttpResponse response = oauth.httpClient().get().execute(get)) {
            String userInfoJwt = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            assertSignedWithKey(userInfoJwt, expectedKid, expectedPublicKeyPem);
        }
    }

    @Test
    public void testAuthorizationResponseSigningKey() throws Exception {
        oauth.responseMode("query.jwt");
        try {
            // Without KID — should use realm active key
            configureTestApp(OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_ALG, Algorithm.RS256);

            AuthorizationEndpointResponse response = oauth.loginForm().doLogin("test-user@localhost", "password");
            assertTrue(response.isRedirected(), "default (no KID): Response should be redirected");
            assertSignedWithKey(response.getResponse(), realmActiveKeyKid, realmActiveKeyPublicKeyPem);

            // With KID — should use configured key (SSO session exists, so auto-redirect)
            configureTestApp(
                OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_ALG, Algorithm.RS256,
                OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_KID, newKeyKid);

            response = oauth.loginForm().doLoginWithCookie();
            assertTrue(response.isRedirected(), "client-specific key: Response should be redirected");
            assertSignedWithKey(response.getResponse(), newKeyKid, newKeyPublicKeyPem);
        } finally {
            oauth.responseMode(null);
        }
    }

    @Test
    public void testDifferentKeysForIdAndAccessToken() throws Exception {
        String key2ProviderId = createRs256KeyProvider(realmResource, "test-key-2");
        realm.cleanup().add(r -> r.components().component(key2ProviderId).remove());

        KeysMetadataRepresentation keysMetadata = realmResource.keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation key2Meta = findKeyByProviderId(keysMetadata, key2ProviderId);
        String key2Kid = key2Meta.getKid();
        String key2PublicKeyPem = key2Meta.getPublicKey();

        configureTestApp(
            OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_KID, newKeyKid,
            OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, key2Kid);

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertSignedWithKey(tokenResponse.getIdToken(), newKeyKid, newKeyPublicKeyPem);
        assertSignedWithKey(tokenResponse.getAccessToken(), key2Kid, key2PublicKeyPem);
    }

    @Test
    public void testMultipleClientsWithDifferentKeys() throws Exception {
        String key2ProviderId = createRs256KeyProvider(realmResource, "test-key-oidc-2");
        realm.cleanup().add(r -> r.components().component(key2ProviderId).remove());

        KeysMetadataRepresentation keysMetadata = realmResource.keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation key2Meta = findKeyByProviderId(keysMetadata, key2ProviderId);
        String key2Kid = key2Meta.getKid();
        String key2PublicKeyPem = key2Meta.getPublicKey();

        configureTestApp(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, newKeyKid);

        // Configure second client with key2
        ClientRepresentation client2Rep = multipleKeysClient.admin().toRepresentation();
        client2Rep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, key2Kid);
        multipleKeysClient.admin().update(client2Rep);

        AccessTokenResponse response1 = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        AccessTokenResponse response2 = oauth.client(multipleKeysClient.getClientId(), multipleKeysClient.getSecret())
            .doPasswordGrantRequest("test-user@localhost", "password");

        // Reset oauth client to default
        oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);

        assertSignedWithKey(response1.getAccessToken(), newKeyKid, newKeyPublicKeyPem);
        assertSignedWithKey(response2.getAccessToken(), key2Kid, key2PublicKeyPem);
    }

    @Test
    public void testKeycloakVerifiesAccessTokenSignedWithClientKey() throws Exception {
        configureTestApp(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, newKeyKid);

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertSignedWithKey(tokenResponse.getAccessToken(), newKeyKid, newKeyPublicKeyPem);

        // UserInfo endpoint must verify the access token signed with the client-specific key
        int userInfoStatus = oauth.userInfoRequest(tokenResponse.getAccessToken()).send().getStatusCode();
        assertEquals(200, userInfoStatus, "UserInfo request should succeed");

        // Token Introspection must verify the access token signed with the client-specific key
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        JsonNode jsonNode = introspectionResponse.asJsonNode();
        assertTrue(jsonNode.get("active").asBoolean(), "Introspection should report token as active");
        assertEquals("test-user@localhost", jsonNode.get("username").asText(),
            "Introspection should return correct username");
    }

    @Test
    public void testFallbackToDefaultKeyWhenConfiguredKeyNotFound() throws Exception {
        configureTestApp(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, "non-existent-key-id");

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertSignedWithKey(tokenResponse.getAccessToken(), realmActiveKeyKid, realmActiveKeyPublicKeyPem);
    }

    @Test
    public void testFallbackWhenKeyAlgorithmMismatch() throws Exception {
        String ecKeyProviderId = createEs256KeyProvider(realmResource, "test-ec-key");
        realm.cleanup().add(r -> r.components().component(ecKeyProviderId).remove());

        KeysMetadataRepresentation keysMetadata = realmResource.keys().getKeyMetadata();
        String ecKeyKid = findKeyByProviderId(keysMetadata, ecKeyProviderId).getKid();

        configureTestApp(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_KID, ecKeyKid);

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertSignedWithKey(tokenResponse.getAccessToken(), realmActiveKeyKid, realmActiveKeyPublicKeyPem);
    }

    private void assertSignedWithKey(String jwt, String expectedKid, String expectedPublicKeyPem) throws Exception {
        JWSInput jws = new JWSInput(jwt);
        assertEquals(expectedKid, jws.getHeader().getKeyId(), "Token should be signed with expected key (kid)");
        PublicKey publicKey = PemUtils.decodePublicKey(expectedPublicKeyPem);
        assertTrue(RSAProvider.verify(jws, publicKey), "Signature verification should succeed with expected key");

        // If the expected key differs from the realm's active key, verify it does NOT verify with realm key
        if (!expectedKid.equals(realmActiveKeyKid)) {
            PublicKey realmKey = PemUtils.decodePublicKey(realmActiveKeyPublicKeyPem);
            assertFalse(RSAProvider.verify(jws, realmKey),
                "Signature verification with realm's active key should fail");
        }
    }

    private void configureTestApp(String... attributePairs) {
        ClientResource clientResource = findClientResource(oauth.getClientId());
        ClientRepresentation rep = clientResource.toRepresentation();
        Map<String, String> attrs = rep.getAttributes();
        if (attrs == null) {
            attrs = new HashMap<>();
            rep.setAttributes(attrs);
        }
        for (int i = 0; i < attributePairs.length; i += 2) {
            if (attributePairs[i + 1] != null) {
                attrs.put(attributePairs[i], attributePairs[i + 1]);
            }
        }
        clientResource.update(rep);
    }

    private ClientResource findClientResource(String clientId) {
        return realmResource.clients().findByClientId(clientId).stream()
            .findFirst()
            .map(c -> realmResource.clients().get(c.getId()))
            .orElseThrow(() -> new IllegalStateException("Client not found: " + clientId));
    }

    private KeysMetadataRepresentation.KeyMetadataRepresentation findKeyByKid(
            KeysMetadataRepresentation keysMetadata, String kid) {
        return keysMetadata.getKeys().stream()
            .filter(k -> kid.equals(k.getKid()))
            .findFirst().orElse(null);
    }

    private KeysMetadataRepresentation.KeyMetadataRepresentation findKeyByProviderId(
            KeysMetadataRepresentation keysMetadata, String providerId) {
        return keysMetadata.getKeys().stream()
            .filter(k -> providerId.equals(k.getProviderId()))
            .findFirst().orElse(null);
    }

    private String createRs256KeyProvider(RealmResource realmResource, String name) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realmResource.toRepresentation().getId());
        rep.setProviderId(RSA_GENERATED_PROVIDER_ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, TEST_KEY_PRIORITY);
        rep.getConfig().putSingle(Attributes.ENABLED_KEY, "true");
        rep.getConfig().putSingle(Attributes.ACTIVE_KEY, "true");
        rep.getConfig().putSingle(Attributes.KEY_SIZE_KEY, "2048");
        rep.getConfig().putSingle(Attributes.ALGORITHM_KEY, Algorithm.RS256);

        Response response = realmResource.components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        return id;
    }

    private String createEs256KeyProvider(RealmResource realmResource, String name) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realmResource.toRepresentation().getId());
        rep.setProviderId("ecdsa-generated");
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, TEST_KEY_PRIORITY);
        rep.getConfig().putSingle(Attributes.ENABLED_KEY, "true");
        rep.getConfig().putSingle(Attributes.ACTIVE_KEY, "true");
        rep.getConfig().putSingle(GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, "P-256");

        Response response = realmResource.components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        return id;
    }

    private static String extractLogoutToken(String formBody) {
        for (String param : formBody.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "logout_token".equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public static class MultipleKeysOAuthClientConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-app-multiple-keys")
                    .directAccessGrantsEnabled(true)
                    .secret("password");
        }
    }
}
