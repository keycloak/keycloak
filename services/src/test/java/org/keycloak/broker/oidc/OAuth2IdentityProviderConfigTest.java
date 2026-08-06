package org.keycloak.broker.oidc;

import java.lang.reflect.Proxy;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OAuth2IdentityProviderConfigTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    private OAuth2IdentityProviderConfig config() {
        return new OAuth2IdentityProviderConfig(new IdentityProviderModel());
    }

    @Test
    public void tlsClientAuthDetected() {
        OAuth2IdentityProviderConfig c = config();
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        assertTrue(c.isTlsClientAuth());
        assertFalse(c.isJWTAuthentication());
        assertFalse(c.isBasicAuthentication());
    }

    @Test
    public void tlsClientAuthNotDetectedForSecretPost() {
        OAuth2IdentityProviderConfig c = config();
        c.setClientAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_POST);
        assertFalse(c.isTlsClientAuth());
    }

    @Test
    public void clientCertKeyProviderRoundTrips() {
        OAuth2IdentityProviderConfig c = config();
        c.setClientCertKeyProviderId("key-provider-abc");
        assertEquals("key-provider-abc", c.getClientCertKeyProviderId());
    }

    @Test
    public void mtlsAliasesUsedOnlyForTlsClientAuth() {
        OAuth2IdentityProviderConfig c = config();
        c.setTokenUrl("https://idp/token");
        c.setUserInfoUrl("https://idp/userinfo");
        c.setTokenIntrospectionUrl("https://idp/introspect");
        c.setMtlsTokenUrl("https://mtls.idp/token");
        c.setMtlsUserInfoUrl("https://mtls.idp/userinfo");
        c.setMtlsTokenIntrospectionUrl("https://mtls.idp/introspect");

        // Without tls_client_auth the regular endpoints are used, even if mTLS aliases are present.
        c.setClientAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_POST);
        assertEquals("https://idp/token", c.getTokenUrlForClientAuth());
        assertEquals("https://idp/userinfo", c.getUserInfoUrlForClientAuth());
        assertEquals("https://idp/introspect", c.getTokenIntrospectionUrlForClientAuth());

        // With tls_client_auth the mTLS aliases take precedence.
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        assertEquals("https://mtls.idp/token", c.getTokenUrlForClientAuth());
        assertEquals("https://mtls.idp/userinfo", c.getUserInfoUrlForClientAuth());
        assertEquals("https://mtls.idp/introspect", c.getTokenIntrospectionUrlForClientAuth());
    }

    @Test
    public void tlsClientAuthFallsBackToRegularEndpointsWhenNoAlias() {
        OAuth2IdentityProviderConfig c = config();
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        c.setTokenUrl("https://idp/token");
        c.setUserInfoUrl("https://idp/userinfo");
        c.setTokenIntrospectionUrl("https://idp/introspect");
        // no mTLS aliases configured

        assertEquals("https://idp/token", c.getTokenUrlForClientAuth());
        assertEquals("https://idp/userinfo", c.getUserInfoUrlForClientAuth());
        assertEquals("https://idp/introspect", c.getTokenIntrospectionUrlForClientAuth());
    }

    private static org.keycloak.models.RealmModel realmWithSsl(org.keycloak.common.enums.SslRequired sslRequired) {
        return (org.keycloak.models.RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                OAuth2IdentityProviderConfigTest.class.getClassLoader(),
                new Class<?>[] { org.keycloak.models.RealmModel.class },
                (proxy, method, args) -> {
                    if ("getSslRequired".equals(method.getName())) {
                        return sslRequired;
                    }
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
    }

    @Test
    public void tlsClientAuthRequiresKeyProvider() {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        org.keycloak.models.RealmModel realm = realmWithSsl(org.keycloak.common.enums.SslRequired.NONE);
        try {
            c.validate(sessionWithKeys(realm), realm);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("certificate"));
        }
    }

    /**
     * Regression test for the validate(session, realm) delegation: a subclass that only overrides the
     * legacy validate(RealmModel) hook (as OAuth2IdentityProviderFactory and GoogleIdentityProviderConfig
     * do) must still have that override invoked when a caller uses the (session, realm) overload. Otherwise
     * provider-specific checks (e.g. required User Info/claim fields, Google's issuer check) are silently
     * skipped when RepresentationToModel calls validate(session, realm).
     */
    @Test
    public void validateWithSessionInvokesLegacyRealmValidateOverride() {
        final boolean[] legacyCalled = { false };
        OAuth2IdentityProviderConfig c = new OAuth2IdentityProviderConfig(new IdentityProviderModel()) {
            @Override
            public void validate(org.keycloak.models.RealmModel realm) {
                legacyCalled[0] = true;
                super.validate(realm);
            }
        };
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        org.keycloak.models.RealmModel realm = realmWithSsl(org.keycloak.common.enums.SslRequired.NONE);

        c.validate(sessionWithKeys(realm), realm);

        assertTrue("validate(session, realm) must invoke the legacy validate(realm) override", legacyCalled[0]);
    }

    /**
     * Regression test: the realm-only URL/PKCE checks must remain reachable through the public
     * validate(RealmModel) hook. Extension code and subclasses that call super.validate(realm) rely on
     * these checks; moving them exclusively into the (session, realm) overload silently dropped them for
     * that call path.
     */
    @Test
    public void validateRealmOnlyRunsUrlChecks() {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("http://insecure/auth");
        org.keycloak.models.RealmModel realm = realmWithSsl(org.keycloak.common.enums.SslRequired.ALL);
        try {
            c.validate(realm);
            org.junit.Assert.fail("expected IllegalArgumentException for insecure authorization_url");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("authorization_url"));
        }
    }

    /**
     * Regression test: the PKCE method check must also remain reachable through validate(RealmModel).
     */
    @Test
    public void validateRealmOnlyRunsPkceCheck() {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setPkceEnabled(true);
        c.setPkceMethod("unsupported");
        org.keycloak.models.RealmModel realm = realmWithSsl(org.keycloak.common.enums.SslRequired.NONE);
        try {
            c.validate(realm);
            org.junit.Assert.fail("expected IllegalArgumentException for unsupported PKCE method");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("PKCE Method not supported"));
        }
    }

    private static org.keycloak.models.RealmModel realmWithSslAndComponent(
            org.keycloak.common.enums.SslRequired sslRequired,
            String componentId,
            ComponentModel component) {
        return (org.keycloak.models.RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                OAuth2IdentityProviderConfigTest.class.getClassLoader(),
                new Class<?>[] { org.keycloak.models.RealmModel.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getSslRequired": return sslRequired;
                        case "getComponent":
                            return (args != null && componentId.equals(args[0])) ? component : null;
                        default:
                            throw new UnsupportedOperationException("stub: " + method.getName());
                    }
                });
    }

    @Test
    public void tlsClientAuthAcceptedWhenKeyProviderExists() throws Exception {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        c.setClientCertKeyProviderId("kp-1");

        ComponentModel cm = new ComponentModel();
        cm.setId("kp-1");
        cm.setProviderType(KeyProvider.class.getName());

        org.keycloak.models.RealmModel realm =
                realmWithSslAndComponent(org.keycloak.common.enums.SslRequired.NONE, "kp-1", cm);

        // Validation now also resolves the key material, so the stubbed session must expose an enabled
        // key (matching provider id) with a private key and a certificate.
        KeyWrapper key = usableKey("kp-1");

        // should not throw
        c.validate(sessionWithKeys(realm, key), realm);
    }

    @Test
    public void tlsClientAuthRejectedWhenKeyMaterialUnusable() {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        c.setClientCertKeyProviderId("kp-1");

        ComponentModel cm = new ComponentModel();
        cm.setId("kp-1");
        cm.setProviderType(KeyProvider.class.getName());

        org.keycloak.models.RealmModel realm =
                realmWithSslAndComponent(org.keycloak.common.enums.SslRequired.NONE, "kp-1", cm);

        // The component exists and is of the right type, but the realm has no enabled/usable key for it
        // (e.g. a disabled provider or one without a private key/certificate). Validation must reject it.
        try {
            c.validate(sessionWithKeys(realm), realm);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("cannot be used"));
        }
    }

    @Test
    public void tlsClientAuthRejectedWhenKeyProviderMissing() {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        c.setClientCertKeyProviderId("ghost");

        org.keycloak.models.RealmModel realm =
                realmWithSslAndComponent(org.keycloak.common.enums.SslRequired.NONE, "other-id", null);

        try {
            c.validate(sessionWithKeys(realm), realm);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("does not exist"));
        }
    }

    @Test
    public void tlsClientAuthRejectsInsecureMtlsAliasWhenSslRequired() {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        // An insecure mTLS alias must not slip past validation: it is preferred for backchannel calls
        // and would otherwise receive the client certificate and credentials over plain HTTP.
        c.setMtlsTokenUrl("http://mtls.idp/token");

        org.keycloak.models.RealmModel realm = realmWithSsl(org.keycloak.common.enums.SslRequired.ALL);
        try {
            c.validate(sessionWithKeys(realm), realm);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("mtls_token_url"));
            assertTrue(expected.getMessage().toLowerCase().contains("secure connections"));
        }
    }

    @Test
    public void tlsClientAuthAcceptsSecureMtlsAliasesWhenSslRequired() throws Exception {
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        c.setClientCertKeyProviderId("kp-1");
        c.setMtlsTokenUrl("https://mtls.idp/token");
        c.setMtlsUserInfoUrl("https://mtls.idp/userinfo");
        c.setMtlsTokenIntrospectionUrl("https://mtls.idp/introspect");

        ComponentModel cm = new ComponentModel();
        cm.setId("kp-1");
        cm.setProviderType(KeyProvider.class.getName());

        org.keycloak.models.RealmModel realm =
                realmWithSslAndComponent(org.keycloak.common.enums.SslRequired.ALL, "kp-1", cm);

        KeyWrapper key = usableKey("kp-1");

        // should not throw: all mTLS aliases use https
        c.validate(sessionWithKeys(realm, key), realm);
    }

    @Test
    public void tlsClientAuthAcceptsEmptyMtlsAliasesWhenSslRequired() throws Exception {
        // A discovery import without mtls_endpoint_aliases stores empty strings to clear stale aliases
        // (OIDCIdentityProviderFactory.parseOIDCConfig). Those blank values must be treated as "no alias"
        // during validation rather than being fed to checkUrl, which would reject "" as a malformed URL.
        OAuth2IdentityProviderConfig c = config();
        c.setAuthorizationUrl("https://idp/auth");
        c.setTokenUrl("https://idp/token");
        c.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        c.setClientCertKeyProviderId("kp-1");
        c.setMtlsTokenUrl("");
        c.setMtlsUserInfoUrl("");
        c.setMtlsTokenIntrospectionUrl("");

        ComponentModel cm = new ComponentModel();
        cm.setId("kp-1");
        cm.setProviderType(KeyProvider.class.getName());

        org.keycloak.models.RealmModel realm =
                realmWithSslAndComponent(org.keycloak.common.enums.SslRequired.ALL, "kp-1", cm);

        KeyWrapper key = usableKey("kp-1");

        // should not throw: blank aliases are skipped, backchannel falls back to the regular endpoints
        c.validate(sessionWithKeys(realm, key), realm);
    }

    // --- test stubs -------------------------------------------------------------------------------

    /**
     * A KeyWrapper that satisfies the mTLS requirements: matching provider id, enabled status, a real
     * private key and a self-signed certificate.
     */
    private static KeyWrapper usableKey(String providerId) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(kp, "CN=" + providerId);

        KeyWrapper key = new KeyWrapper();
        key.setProviderId(providerId);
        key.setStatus(KeyStatus.ACTIVE);
        key.setPrivateKey(kp.getPrivate());
        key.setPublicKey(kp.getPublic());
        key.setCertificate(cert);
        return key;
    }

    /**
     * Stubs a {@link KeycloakSession} whose {@code keys().getKeysStream(realm)} returns the given keys.
     */
    private static KeycloakSession sessionWithKeys(org.keycloak.models.RealmModel realm, KeyWrapper... keys) {
        KeyManager keyManager = (KeyManager) Proxy.newProxyInstance(
                OAuth2IdentityProviderConfigTest.class.getClassLoader(),
                new Class<?>[] { KeyManager.class },
                (proxy, method, args) -> {
                    if ("getKeysStream".equals(method.getName())
                            && args != null && args.length == 1) {
                        return Stream.of(keys);
                    }
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
        return (KeycloakSession) Proxy.newProxyInstance(
                OAuth2IdentityProviderConfigTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("keys".equals(method.getName())) {
                        return keyManager;
                    }
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
    }
}
