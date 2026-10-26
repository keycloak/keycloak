package org.keycloak.tests.broker.oidc;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.EventType;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientCertificateRepresentation;
import org.keycloak.representations.idm.ClientCertificateTestRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The consumer realm authenticates to the provider realm using mutual TLS (RFC 8705 {@code tls_client_auth}). The
 * server presents the client certificate of an {@code rsa} key provider of the consumer realm, and the provider realm
 * client is authenticated by that certificate instead of a secret.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerTlsClientAuthTest extends AbstractKcOidcBrokerTest {

    // alias of the key entry in the managed client keystore
    private static final String CLIENT_KEY_ALIAS = "client-key";

    @InjectCertificates(config = MutualTlsCertificates.class)
    ManagedCertificates managedCertificates;

    @InjectPage
    ErrorPage errorPage;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = TlsClientAuthProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = TlsClientAuthConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectEvents(realmRef = "provider")
    Events providerEvents;

    /**
     * Adds the managed client certificate as a key provider to the consumer realm, and lets the identity provider use it.
     */
    @BeforeEach
    void configureClientCertificate() throws Exception {
        KeyStore keyStore = managedCertificates.getClientKeyStore();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(CLIENT_KEY_ALIAS, managedCertificates.getClientKeyStorePassword().toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(CLIENT_KEY_ALIAS);

        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName("client-certificate");
        keyProvider.setProviderId("rsa");
        keyProvider.setProviderType(KeyProvider.class.getName());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", "100");
        config.putSingle("privateKey", PemUtils.encodeKey(privateKey));
        config.putSingle("certificate", PemUtils.encodeCertificate(certificate));
        keyProvider.setConfig(config);

        String keyProviderId;
        try (Response response = consumerRealm.admin().components().add(keyProvider)) {
            keyProviderId = CreatedResponseUtil.getCreatedId(response);
        }

        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().put("clientCertificateKeyProviderId", keyProviderId);
        idp.update(rep);
    }

    @Test
    void missingClientCertificateKeyProviderIsRejectedWhenSaving() {
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().remove("clientCertificateKeyProviderId");

        assertUpdateRejected(idp, rep, "is required");
    }

    @Test
    void mtlsEndpointAliasesAreImportedFromTheDiscoveryDocument() {
        String discovery = providerRealm.getBaseUrl() + "/.well-known/openid-configuration";
        for (String providerId : List.of("oidc", "oauth2")) {
            Map<String, String> config = consumerRealm.admin().identityProviders()
                    .importFrom(Map.of("providerId", providerId, "fromUrl", discovery));

            // the provider publishes the same URLs as its mTLS endpoint aliases
            Assertions.assertNotNull(config.get("mtlsTokenUrl"), providerId);
            Assertions.assertEquals(config.get("tokenUrl"), config.get("mtlsTokenUrl"), providerId);
            Assertions.assertNotNull(config.get("mtlsUserInfoUrl"), providerId);
            Assertions.assertEquals(config.get("userInfoUrl"), config.get("mtlsUserInfoUrl"), providerId);
            Assertions.assertNotNull(config.get("mtlsTokenIntrospectionUrl"), providerId);
            Assertions.assertEquals(config.get("tokenIntrospectionUrl"), config.get("mtlsTokenIntrospectionUrl"), providerId);
        }
    }

    @Test
    void mtlsTokenEndpointAliasIsUsedForTheTokenRequest() {
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        // the regular token endpoint is unreachable, so the login only works through the alias
        rep.getConfig().put("clientSecret", getBrokerClientSecret());
        rep.getConfig().put("mtlsTokenUrl", rep.getConfig().get("tokenUrl"));
        rep.getConfig().put("tokenUrl", "https://localhost:1/unreachable");
        idp.update(rep);

        loginUser();
    }

    @Test
    void loginFailsWhenTheClientCertificateKeyProviderIsDisabled() {
        disableClientCertificateKeyProvider();

        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();

        errorPage.assertCurrent();
    }

    /**
     * The stored token of the provider is refreshed when the broker token endpoint is read. The refresh token is bound
     * to the client certificate, so the refresh request only succeeds when the broker presents it.
     */
    @Test
    void storedTokenIsRefreshedWithTheClientCertificate() throws Exception {
        // keep the tokens of the provider, and refresh them as soon as they are read
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.setStoreToken(true);
        rep.getConfig().put(IdentityProviderModel.MIN_VALIDITY_TOKEN, "86400");
        idp.update(rep);

        loginUser();

        // the user is allowed to read the stored token
        UserRepresentation user = consumerRealm.admin().users().search(getUserLogin()).get(0);
        ClientRepresentation brokerClient = consumerRealm.admin().clients().findByClientId(Constants.BROKER_SERVICE_CLIENT_ID).get(0);
        RoleRepresentation readToken = consumerRealm.admin().clients().get(brokerClient.getId()).roles().get(Constants.READ_TOKEN_ROLE).toRepresentation();
        consumerRealm.admin().users().get(user.getId()).roles().clientLevel(brokerClient.getId()).add(List.of(readToken));

        // log in again for an access token that has the role
        oauth.openLoginForm();
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, tokens.getStatusCode(), tokens.getErrorDescription());
        // refresh token events are not saved by default
        providerRealm.updateWithCleanup(r -> r.enabledEventTypes(EventType.REFRESH_TOKEN.name()));
        providerEvents.skipAll();

        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(managedCertificates.getClientSSLContext()).build()) {
            HttpGet request = new HttpGet(consumerRealm.getBaseUrl() + "/broker/" + getIdpAlias() + "/token");
            request.setHeader("Authorization", "Bearer " + tokens.getAccessToken());
            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode(), body);
                Assertions.assertNotNull(JsonSerialization.mapper.readTree(body).get("access_token"), body);
            }
        }

        // the provider refreshed the token, a stored token returned as it is would not prove the refresh
        EventAssertion.assertSuccess(providerEvents.poll())
                .type(EventType.REFRESH_TOKEN)
                .clientId(CLIENT_ID);
    }

    private IdentityProviderRepresentation tlsClientAuthIdp(String alias, String keyProviderId) {
        IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
        rep.setAlias(alias);
        rep.setProviderId("oidc");
        rep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("clientId", "mtls-client");
        config.put("clientAuthMethod", OIDCLoginProtocol.TLS_CLIENT_AUTH);
        config.put("clientCertificateKeyProviderId", keyProviderId);
        config.put("authorizationUrl", "https://localhost:8443/auth");
        config.put("tokenUrl", "https://localhost:8443/token");
        rep.setConfig(config);
        return rep;
    }

    private ComponentRepresentation clientCertificateKeyProvider() {
        return consumerRealm.admin().components().query(null, KeyProvider.class.getName()).stream()
                .filter(component -> "client-certificate".equals(component.getName()))
                .findFirst().orElseThrow();
    }

    private String clientCertificateKeyProviderId() {
        return clientCertificateKeyProvider().getId();
    }

    private String disableClientCertificateKeyProvider() {
        ComponentRepresentation keyProvider = clientCertificateKeyProvider();
        keyProvider.getConfig().putSingle("enabled", "false");
        consumerRealm.admin().components().component(keyProvider.getId()).update(keyProvider);
        return keyProvider.getId();
    }

    private String createKeyProvider(String name, String providerId) {
        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName(name);
        keyProvider.setProviderId(providerId);
        keyProvider.setProviderType(KeyProvider.class.getName());
        keyProvider.setConfig(new MultivaluedHashMap<>());
        try (Response response = consumerRealm.admin().components().add(keyProvider)) {
            return CreatedResponseUtil.getCreatedId(response);
        }
    }

    private void assertCreateRejected(IdentityProviderRepresentation rep, String expectedError) {
        try (Response response = consumerRealm.admin().identityProviders().create(rep)) {
            Assertions.assertEquals(400, response.getStatus());
            String error = response.readEntity(ErrorRepresentation.class).getErrorMessage();
            Assertions.assertTrue(error.contains(expectedError), error);
        }
    }

    private void assertUpdateRejected(IdentityProviderResource idp, IdentityProviderRepresentation rep, String expectedError) {
        BadRequestException e = Assertions.assertThrows(BadRequestException.class, () -> idp.update(rep));
        String error = e.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage();
        Assertions.assertTrue(error.contains(expectedError), error);
    }

    @Test
    void unknownClientCertificateKeyProviderIsRejectedWhenCreating() {
        assertCreateRejected(tlsClientAuthIdp("unknown-key-provider", "does-not-exist"), "'does-not-exist' was not found");
    }

    @Test
    void secretKeyProviderIsRejectedWhenCreating() {
        assertCreateRejected(tlsClientAuthIdp("hmac-key-provider", createKeyProvider("hmac", "hmac-generated")), "has no private key");
    }

    @Test
    void componentThatIsNotAKeyProviderIsRejectedWhenCreating() {
        ComponentRepresentation other = new ComponentRepresentation();
        other.setName("not-a-key-provider");
        other.setProviderId("declarative-user-profile");
        other.setProviderType("org.keycloak.userprofile.UserProfileProvider");
        other.setConfig(new MultivaluedHashMap<>());
        String otherId;
        try (Response response = consumerRealm.admin().components().add(other)) {
            otherId = CreatedResponseUtil.getCreatedId(response);
        }

        assertCreateRejected(tlsClientAuthIdp("other-component", otherId), "is not a key provider");
    }

    @Test
    void changingTheClientCertificateKeyProviderToAnUnknownOneIsRejected() {
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().put("clientSecret", getBrokerClientSecret());
        rep.getConfig().put("clientCertificateKeyProviderId", "does-not-exist");

        assertUpdateRejected(idp, rep, "was not found");
    }

    @Test
    void switchingToTlsClientAuthChecksAnUnchangedKeyProvider() {
        // not checked while another client authentication method is used
        IdentityProviderRepresentation rep = tlsClientAuthIdp("switched", "does-not-exist");
        rep.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
        rep.getConfig().put("clientSecret", "secret");
        consumerRealm.admin().identityProviders().create(rep).close();

        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get("switched");
        IdentityProviderRepresentation update = idp.toRepresentation();
        update.getConfig().put("clientAuthMethod", OIDCLoginProtocol.TLS_CLIENT_AUTH);
        update.getConfig().remove("clientSecret");

        assertUpdateRejected(idp, update, "was not found");
    }

    @Test
    void unchangedKeyProviderThatWasDeletedDoesNotPreventOtherChanges() {
        consumerRealm.admin().components().component(clientCertificateKeyProviderId()).remove();

        // the stale reference is kept, and the identity provider can still be disabled
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.setEnabled(false);
        idp.update(rep);
        Assertions.assertFalse(idp.toRepresentation().isEnabled());

        // choosing another unknown key provider is still rejected
        IdentityProviderRepresentation changed = idp.toRepresentation();
        changed.getConfig().put("clientCertificateKeyProviderId", "does-not-exist");
        assertUpdateRejected(idp, changed, "was not found");
    }

    @Test
    void keyProviderThatDoesNotExistYetIsImportedWithPartialImport() {
        PartialImportRepresentation partialImport = new PartialImportRepresentation();
        partialImport.setIfResourceExists(PartialImportRepresentation.Policy.FAIL.name());
        partialImport.setIdentityProviders(List.of(tlsClientAuthIdp("partial-import-idp", "created-later")));

        try (Response response = consumerRealm.admin().partialImport(partialImport)) {
            Assertions.assertEquals(200, response.getStatus());
        }
        Assertions.assertEquals("created-later", consumerRealm.admin().identityProviders().get("partial-import-idp")
                .toRepresentation().getConfig().get("clientCertificateKeyProviderId"));
    }

    @Test
    void clientCertificatesAreListedWithoutKeyMaterial() throws Exception {
        // not usable as the client certificate: it is a secret key
        createKeyProvider("hmac", "hmac-generated");

        List<ClientCertificateRepresentation> certificates = consumerRealm.admin().identityProviders().getClientCertificates();

        ClientCertificateRepresentation certificate = certificates.stream()
                .filter(c -> "client-certificate".equals(c.getProviderName())).findFirst().orElseThrow();
        Assertions.assertEquals(clientCertificateKeyProviderId(), certificate.getProviderId());
        Assertions.assertEquals("RSA", certificate.getType());
        Assertions.assertEquals("ACTIVE", certificate.getStatus());
        Assertions.assertNotNull(certificate.getSubjectDn());
        Assertions.assertNotNull(certificate.getIssuerDn());
        Assertions.assertNotNull(certificate.getSerialNumber());
        Assertions.assertNotNull(certificate.getValidFrom());
        Assertions.assertNotNull(certificate.getValidTo());

        X509Certificate expected = (X509Certificate) managedCertificates.getClientKeyStore().getCertificate(CLIENT_KEY_ALIAS);
        HexFormat hex = HexFormat.ofDelimiter(":").withUpperCase();
        Assertions.assertEquals(hex.formatHex(MessageDigest.getInstance("SHA-256").digest(expected.getEncoded())), certificate.getFingerprintSha256());

        Assertions.assertTrue(certificates.stream().noneMatch(c -> "hmac".equals(c.getProviderName())));
        String json = JsonSerialization.writeValueAsString(certificates);
        for (String field : List.of("privateKey", "publicKey", "\"certificate\"", "BEGIN")) {
            Assertions.assertFalse(json.contains(field), field);
        }
    }

    private RealmResource realmAs(String username, String adminRole) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@localhost");
        user.setFirstName(username);
        user.setLastName(username);
        user.setEnabled(true);
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        String userId;
        try (Response response = consumerRealm.admin().users().create(user)) {
            userId = CreatedResponseUtil.getCreatedId(response);
        }
        ClientRepresentation realmManagement = consumerRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation role = consumerRealm.admin().clients().get(realmManagement.getId()).roles().get(adminRole).toRepresentation();
        consumerRealm.admin().users().get(userId).roles().clientLevel(realmManagement.getId()).add(List.of(role));

        return adminClientFactory.create().realm(consumerRealm.getName()).clientId("admin-cli")
                .username(username).password("password").autoClose().build().realm(consumerRealm.getName());
    }

    @Test
    void clientCertificatesAreListedForAdminsWithOnlyTheManageIdentityProvidersRole() {
        RealmResource realm = realmAs("idp-admin", AdminRoles.MANAGE_IDENTITY_PROVIDERS);

        // the keys of the realm are not visible to this admin, the client certificates are
        Assertions.assertThrows(ForbiddenException.class, () -> realm.keys().getKeyMetadata());
        Assertions.assertTrue(realm.identityProviders().getClientCertificates().stream()
                .anyMatch(c -> "client-certificate".equals(c.getProviderName())));
    }

    @Test
    void clientCertificatesAreListedButNotTestedWithTheViewIdentityProvidersRole() {
        RealmResource realm = realmAs("view-admin", AdminRoles.VIEW_IDENTITY_PROVIDERS);

        Assertions.assertTrue(realm.identityProviders().getClientCertificates().stream()
                .anyMatch(c -> "client-certificate".equals(c.getProviderName())));

        ClientCertificateTestRepresentation request = new ClientCertificateTestRepresentation();
        request.setKeyProviderId(clientCertificateKeyProviderId());
        request.setTokenUrl(providerRealm.getBaseUrl() + "/protocol/openid-connect/token");
        try (Response response = realm.identityProviders().testClientCertificate(request)) {
            Assertions.assertEquals(403, response.getStatus());
        }
    }

    @Test
    void clientCertificatesAreNotListedWithoutAnIdentityProvidersRole() {
        RealmResource realm = realmAs("users-admin", AdminRoles.VIEW_USERS);

        Assertions.assertThrows(ForbiddenException.class, () -> realm.identityProviders().getClientCertificates());
    }

    private void assertTestClientCertificateError(String keyProviderId, String tokenUrl, String expectedError) {
        ClientCertificateTestRepresentation request = new ClientCertificateTestRepresentation();
        request.setKeyProviderId(keyProviderId);
        request.setClientId(CLIENT_ID);
        request.setTokenUrl(tokenUrl);
        try (Response response = consumerRealm.admin().identityProviders().testClientCertificate(request)) {
            Assertions.assertEquals(400, response.getStatus());
            String error = response.readEntity(ErrorRepresentation.class).getErrorMessage();
            Assertions.assertTrue(error.contains(expectedError), error);
        }
    }

    private ClientCertificateTestRepresentation testRequest(String clientId, String tokenUrl) {
        ClientCertificateTestRepresentation request = new ClientCertificateTestRepresentation();
        request.setKeyProviderId(clientCertificateKeyProviderId());
        request.setClientId(clientId);
        request.setTokenUrl(tokenUrl);
        return request;
    }

    @Test
    void testOfTheClientCertificateSucceedsWhenTheClientIsAuthenticated() {
        // the token endpoint authenticates the client before it rejects the code with invalid_grant
        try (Response response = consumerRealm.admin().identityProviders()
                .testClientCertificate(testRequest(CLIENT_ID, providerRealm.getBaseUrl() + "/protocol/openid-connect/token"))) {
            Assertions.assertEquals(204, response.getStatus());
        }
    }

    @Test
    void testOfTheClientCertificateFailsWhenTheClientIsRejected() {
        try (Response response = consumerRealm.admin().identityProviders()
                .testClientCertificate(testRequest("unknown-client", providerRealm.getBaseUrl() + "/protocol/openid-connect/token"))) {
            Assertions.assertEquals(400, response.getStatus());
            String error = response.readEntity(ErrorRepresentation.class).getErrorMessage();
            Assertions.assertTrue(error.contains("rejected the client certificate"), error);
        }
    }

    @Test
    void testOfTheClientCertificateFailsWhenTheClientIsPublic() {
        ClientRepresentation publicClient = new ClientRepresentation();
        publicClient.setClientId("public-client");
        publicClient.setPublicClient(true);
        providerRealm.admin().clients().create(publicClient).close();

        // a public client gets invalid_grant without being authenticated
        try (Response response = consumerRealm.admin().identityProviders()
                .testClientCertificate(testRequest("public-client", providerRealm.getBaseUrl() + "/protocol/openid-connect/token"))) {
            Assertions.assertEquals(400, response.getStatus());
            String error = response.readEntity(ErrorRepresentation.class).getErrorMessage();
            Assertions.assertTrue(error.contains("without the client certificate"), error);
        }
    }

    @Test
    void testOfTheClientCertificateIsNotConfirmedByAnotherAnswer() {
        // the discovery document does not accept a POST and answers without an OAuth2 error
        try (Response response = consumerRealm.admin().identityProviders()
                .testClientCertificate(testRequest(CLIENT_ID, providerRealm.getBaseUrl() + "/.well-known/openid-configuration"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals(Boolean.FALSE, response.readEntity(Map.class).get("authenticated"));
        }
    }

    @Test
    void testOfTheClientCertificateReportsTheFailure() {
        String keyProviderId = clientCertificateKeyProviderId();
        String tokenUrl = providerRealm.getBaseUrl() + "/protocol/openid-connect/token";

        assertTestClientCertificateError("does-not-exist", tokenUrl, "was not found");
        assertTestClientCertificateError(" ", tokenUrl, "is required");
        assertTestClientCertificateError(keyProviderId, "http://localhost:8080/token", "HTTPS");
        assertTestClientCertificateError(keyProviderId, null, "HTTPS");
        assertTestClientCertificateError(keyProviderId, "https://localhost:1/token", "Could not connect");
    }

    @Test
    void testOfTheClientCertificateWithoutABodyIsABadRequest() {
        try (Response response = consumerRealm.admin().identityProviders().testClientCertificate(null)) {
            Assertions.assertEquals(400, response.getStatus());
        }
    }

    @Test
    void testOfTheClientCertificateOfADisabledKeyProviderFails() {
        String keyProviderId = disableClientCertificateKeyProvider();

        assertTestClientCertificateError(keyProviderId, providerRealm.getBaseUrl() + "/protocol/openid-connect/token", "is disabled");
    }

    static class MutualTlsCertificates implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.mTlsEnabled(true);
        }
    }

    static class TlsClientAuthProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .authenticatorType(X509ClientAuthenticator.PROVIDER_ID)
                            .attribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, "(.*?)(?:$)")
                            // the access token is bound to the client certificate, so the user info endpoint
                            // only accepts it when the same certificate is presented (RFC 8705)
                            .attribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, "true"));
        }
    }

    static class TlsClientAuthConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // the key provider is added by the test, as the client certificate is generated when the test runs
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientAuthMethod", OIDCLoginProtocol.TLS_CLIENT_AUTH)
                            .attribute("clientCertificateKeyProviderId", "to-be-configured"));
        }
    }
}
