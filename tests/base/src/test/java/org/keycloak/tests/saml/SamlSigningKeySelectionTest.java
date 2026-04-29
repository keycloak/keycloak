package org.keycloak.tests.saml;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.saml.SamlClient;
import org.keycloak.testframework.saml.annotations.InjectSamlClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.TestRealmUserConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for client-specific signing key selection in SAML protocol.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
@KeycloakIntegrationTest
public class SamlSigningKeySelectionTest {

    private static final String RSA_GENERATED_PROVIDER_ID = "rsa-generated";
    private static final String TEST_KEY_PRIORITY = "50";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    // METHOD lifecycle ensures each test starts with a fresh client, avoiding attribute leakage
    // between tests (Admin API update does not remove attributes that were added).
    @InjectSamlClient(lifecycle = LifeCycle.METHOD)
    SamlClient saml;

    @InjectClient(config = BackchannelLogoutSamlClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient backchannelClient;

    @InjectClient(config = MultipleKeysSamlClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient multipleKeysClient;

    private RealmResource realmResource;
    private String newKeyKid;
    private String newKeyPublicKeyPem;
    private String realmActiveKeyKid;
    private String realmActiveKeyPublicKeyPem;

    @BeforeEach
    public void setupSigningKey() {
        realmResource = realm.admin();

        // Create key provider and register cleanup
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

        assertNotEquals(realmActiveKeyKid, newKeyKid, "Test key must differ from realm active key");
    }

    @Test
    public void testLoginResponseSigningKey() throws Exception {
        // First verify basic login works, then test with signature
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");

        SAMLDocumentHolder response = saml.doLogin("test-user@localhost", "password");
        assertNotNull(response, "SAML response should not be null");
        assertSignedWithKey(response.getSamlDocument(), realmActiveKeyPublicKeyPem, null);

        // With KID — should use configured key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, newKeyKid);

        response = saml.doLogin("test-user@localhost", "password");
        assertNotNull(response, "SAML response should not be null");
        assertSignedWithKey(response.getSamlDocument(), newKeyPublicKeyPem, realmActiveKeyPublicKeyPem);
    }

    @Test
    public void testLogoutWithCookieSignedWithClientSpecificKey() throws Exception {
        // Without KID — should use realm active key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");

        SAMLDocumentHolder loginResponse = saml.doLogin("test-user@localhost", "password");
        NameIDType nameId = saml.extractNameId(loginResponse);
        String sessionIndex = saml.extractSessionIndex(loginResponse);

        SAMLDocumentHolder logoutResponse = saml.doLogout(nameId.getValue(), sessionIndex);
        assertNotNull(logoutResponse, "Logout response should not be null");
        assertSignedWithKey(logoutResponse.getSamlDocument(), realmActiveKeyPublicKeyPem, null);

        // With KID — should use configured key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, newKeyKid);

        loginResponse = saml.doLogin("test-user@localhost", "password");
        nameId = saml.extractNameId(loginResponse);
        sessionIndex = saml.extractSessionIndex(loginResponse);

        logoutResponse = saml.doLogout(nameId.getValue(), sessionIndex);
        assertNotNull(logoutResponse, "Logout response should not be null");
        assertSignedWithKey(logoutResponse.getSamlDocument(), newKeyPublicKeyPem, realmActiveKeyPublicKeyPem);
    }

    @Test
    public void testLogoutWithoutCookieSignedWithClientSpecificKey() throws Exception {
        // Without KID — should use realm active key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");

        SAMLDocumentHolder loginResponse = saml.doLogin("test-user@localhost", "password");
        NameIDType nameId = saml.extractNameId(loginResponse);
        String sessionIndex = saml.extractSessionIndex(loginResponse);

        saml.clearCookies();
        SAMLDocumentHolder logoutResponse = saml.doLogout(nameId.getValue(), sessionIndex);
        assertNotNull(logoutResponse, "Logout response should not be null");
        assertSignedWithKey(logoutResponse.getSamlDocument(), realmActiveKeyPublicKeyPem, null);

        // With KID — should use configured key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, newKeyKid);

        loginResponse = saml.doLogin("test-user@localhost", "password");
        nameId = saml.extractNameId(loginResponse);
        sessionIndex = saml.extractSessionIndex(loginResponse);

        saml.clearCookies();
        logoutResponse = saml.doLogout(nameId.getValue(), sessionIndex);
        assertNotNull(logoutResponse, "Logout response should not be null");
        assertSignedWithKey(logoutResponse.getSamlDocument(), newKeyPublicKeyPem, realmActiveKeyPublicKeyPem);
    }

    @Test
    public void testErrorResponseSigningKey() throws Exception {
        // Without KID — should use realm active key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");

        saml.openLoginForm(true);
        SAMLDocumentHolder response = saml.parseLoginResponse();
        assertNotNull(response, "Error response should not be null");
        assertSignedWithKey(response.getSamlDocument(), realmActiveKeyPublicKeyPem, null);

        // With KID — should use configured key
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, newKeyKid);

        saml.openLoginForm(true);
        response = saml.parseLoginResponse();
        assertNotNull(response, "Error response should not be null");
        assertSignedWithKey(response.getSamlDocument(), newKeyPublicKeyPem, realmActiveKeyPublicKeyPem);
    }

    @Test
    public void testBackchannelLogoutRequestSigningKey() throws Exception {
        // Without KID
        Document defaultDoc = performBackchannelLogoutAndCaptureRequest(null);
        assertSignedWithKey(defaultDoc, realmActiveKeyPublicKeyPem, null);

        // With KID
        Document configuredDoc = performBackchannelLogoutAndCaptureRequest(newKeyKid);
        assertSignedWithKey(configuredDoc, newKeyPublicKeyPem, realmActiveKeyPublicKeyPem);
    }

    /**
     * SAML backchannel logout requires 2 clients: the logout initiator (client 1) and
     * the backchannel target (client 2). Keycloak marks the initiator's session as LOGGED_OUT
     * before processing backchannel, so only other clients receive the SOAP LogoutRequest.
     */
    private Document performBackchannelLogoutAndCaptureRequest(String signingKeyKid) throws Exception {
        saml.clearLastBackchannelLogoutDocument();
        saml.setBackchannelLogoutIssuer(backchannelClient.getClientId());
        String originalClientId = saml.getClientId();

        // Configure backchannel client: SOAP logout URL, signing, frontchannel=false
        ClientRepresentation bclRep = backchannelClient.admin().toRepresentation();
        bclRep.setFrontchannelLogout(false);
        bclRep.getAttributes().put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, saml.getBackchannelLogoutUrl());
        bclRep.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        if (signingKeyKid != null) {
            bclRep.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, signingKeyKid);
        }
        backchannelClient.admin().update(bclRep);

        // Login with client 1
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        SAMLDocumentHolder loginResponse = saml.doLogin("test-user@localhost", "password");
        NameIDType nameId = saml.extractNameId(loginResponse);
        String sessionIndex = saml.extractSessionIndex(loginResponse);

        // Login with backchannel client (SSO)
        saml.client(backchannelClient.getClientId());
        saml.doLogin("test-user@localhost", "password");

        // Logout from client 1 -- triggers backchannel logout to backchannel client
        saml.client(originalClientId);
        saml.doLogout(nameId.getValue(), sessionIndex);

        assertNotNull(saml.getLastBackchannelLogoutDocument(), "Backchannel logout request should be received");
        return saml.getLastBackchannelLogoutDocument();
    }

    @Test
    public void testArtifactResponseSigningKey() throws Exception {
        // Without KID — should use realm active key
        configureClient(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");

        SAMLDocumentHolder response = saml.doArtifactLogin("test-user@localhost", "password");
        assertNotNull(response, "ArtifactResponse should not be null");
        assertSignedWithKey(response.getSamlDocument(), realmActiveKeyPublicKeyPem, null);

        // With KID — should use configured key
        configureClient(SamlConfigAttributes.SAML_ARTIFACT_BINDING, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, newKeyKid);

        response = saml.doArtifactLogin("test-user@localhost", "password");
        assertNotNull(response, "ArtifactResponse should not be null");
        assertSignedWithKey(response.getSamlDocument(), newKeyPublicKeyPem, realmActiveKeyPublicKeyPem);
    }

    @Test
    public void testMultipleClientsWithDifferentKeys() throws Exception {
        String key2ProviderId = createRs256KeyProvider(realmResource, "test-key-2");
        realm.cleanup().add(r -> r.components().component(key2ProviderId).remove());

        KeysMetadataRepresentation keysMetadata = realmResource.keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation key2Meta = findKeyByProviderId(keysMetadata, key2ProviderId);
        String key2Kid = key2Meta.getKid();
        String key2PublicKeyPem = key2Meta.getPublicKey();

        // Configure second client with key2
        ClientRepresentation client2Rep = multipleKeysClient.admin().toRepresentation();
        client2Rep.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        client2Rep.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, key2Kid);
        multipleKeysClient.admin().update(client2Rep);

        // Configure first client with key1
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, newKeyKid);

        // Login with first client
        SAMLDocumentHolder response1 = saml.doLogin("test-user@localhost", "password");
        assertSignedWithKey(response1.getSamlDocument(), newKeyPublicKeyPem, key2PublicKeyPem);

        // Login with second client (SSO)
        saml.client(multipleKeysClient.getClientId());
        SAMLDocumentHolder response2 = saml.doLogin("test-user@localhost", "password");
        assertSignedWithKey(response2.getSamlDocument(), key2PublicKeyPem, newKeyPublicKeyPem);
    }

    @Test
    public void testFallbackToDefaultKeyWhenConfiguredKeyNotFound() throws Exception {
        configureClient(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true",
                SamlConfigAttributes.SAML_SERVER_SIGNATURE_KID, "non-existent-key-id");

        SAMLDocumentHolder response = saml.doLogin("test-user@localhost", "password");
        assertNotNull(response, "SAML response should not be null");
        assertSignedWithKey(response.getSamlDocument(), realmActiveKeyPublicKeyPem, null);
    }

    private void configureClient(String... attributePairs) {
        ClientRepresentation rep = saml.admin().toRepresentation();
        Map<String, String> attrs = rep.getAttributes();
        if (attrs == null) {
            attrs = new HashMap<>();
            rep.setAttributes(attrs);
        }
        for (int i = 0; i < attributePairs.length; i += 2) {
            attrs.put(attributePairs[i], attributePairs[i + 1]);
        }
        saml.admin().update(rep);
    }

    private void assertSignedWithKey(Document doc, String expectedPublicKeyPem, String negativePublicKeyPem) {
        PublicKey expectedKey = PemUtils.decodePublicKey(expectedPublicKeyPem);
        try {
            SamlProtocolUtils.verifyDocumentSignature(doc, new HardcodedKeyLocator(expectedKey));
        } catch (Exception e) {
            fail("Signature verification should succeed: " + e.getMessage());
        }

        if (negativePublicKeyPem != null) {
            PublicKey negativeKey = PemUtils.decodePublicKey(negativePublicKeyPem);
            try {
                SamlProtocolUtils.verifyDocumentSignature(doc, new HardcodedKeyLocator(negativeKey));
                fail("Signature verification with wrong key should fail");
            } catch (Exception e) {
                // Expected
            }
        }
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

    public static class BackchannelLogoutSamlClientConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("http://localhost:8280/test-saml-app-backchannel/")
                    .protocol("saml")
                    .redirectUris("http://127.0.0.1:8500/saml/acs/*")
                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                    .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://127.0.0.1:8500/saml/acs");
            // Note: no SLO URL set here - backchannel test sets SOAP URL dynamically
        }
    }

    public static class MultipleKeysSamlClientConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("http://localhost:8280/test-saml-app-multiple-keys/")
                    .protocol("saml")
                    .redirectUris("http://127.0.0.1:8500/saml/acs/*")
                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                    .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://127.0.0.1:8500/saml/acs")
                    .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "http://127.0.0.1:8500/saml/acs");
        }
    }
}
