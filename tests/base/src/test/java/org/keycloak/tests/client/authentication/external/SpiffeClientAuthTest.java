package org.keycloak.tests.client.authentication.external;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.spiffe.SpiffeConstants;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderConfig;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfig;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfigBuilder;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.UUID;

@KeycloakIntegrationTest(config = SpiffeClientAuthTest.SpiffeServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SpiffeClientAuthTest {

    private static final String IDP_ALIAS = "spiffe-idp";

    private static final String CLIENT_ID = "spiffe://mytrust-domain/myclient";

    @InjectRealm(config = ExernalClientAuthRealmConfig.class)
    protected ManagedRealm realm;

    @InjectClient(config = ExernalClientAuthClientConfig.class)
    protected ManagedClient client;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectOAuthIdentityProvider(config = SpiffeIdpConfig.class)
    OAuthIdentityProvider identityProvider;

    @Test
    public void testInvalidSignature() {
        OAuthIdentityProvider.OAuthIdentityProviderKeys keys = identityProvider.createKeys();
        String jws = identityProvider.encodeToken(createDefaultToken(), keys);
        Assertions.assertFalse(doClientGrant(jws));
    }

    @Test
    public void testValidToken() {
        Assertions.assertTrue(doClientGrant(createDefaultToken()));
    }

    @Test
    public void testInvalidConfig() {
        testInvalidConfig("with-port:8080", "https://localhost");
        testInvalidConfig("spiffe://with-spiffe-scheme", "https://localhost");
        testInvalidConfig("valid", "invalid-url");
    }

    @Test
    public void testInvalidTrustDomain() {
        IdentityProviderUpdater.updateWithRollback(realm, IDP_ALIAS, rep -> {
            rep.getConfig().put(SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY, "different-domain");
        });

        Assertions.assertFalse(doClientGrant(createDefaultToken()));
    }

    @Test
    public void testValidInvalidAssertionType() {
        String jws = identityProvider.encodeToken(createDefaultToken());
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(jws, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT).send();
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void testInvalidAud() {
        JsonWebToken token = createDefaultToken();
        token.audience("invalid");
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testMultipleAud() {
        JsonWebToken token = createDefaultToken();
        token.audience(token.getAudience()[0], "invalid");
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testInvalidNbf() {
        JsonWebToken token = createDefaultToken();
        token.nbf((long) (Time.currentTime() + 60));
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testExpired() {
        JsonWebToken token = createDefaultToken();
        token.exp((long) (Time.currentTime() - 30));
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testMissingExp() {
        JsonWebToken token = createDefaultToken();
        token.exp(null);
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testReuse() {
        JsonWebToken token = createDefaultToken();
        token.id(UUID.randomUUID().toString());
        Assertions.assertTrue(doClientGrant(token));
        Assertions.assertTrue(doClientGrant(token));
    }

    private boolean doClientGrant(JsonWebToken token) {
        String jws = identityProvider.encodeToken(token);
        return doClientGrant(jws);
    }

    private boolean doClientGrant(String jws) {
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(jws, SpiffeConstants.CLIENT_ASSERTION_TYPE).send();
        return response.isSuccess();
    }

    private JsonWebToken createDefaultToken() {
        JsonWebToken token = new JsonWebToken();
        token.id(null);
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.exp((long) (Time.currentTime() + 300));
        token.subject(CLIENT_ID);
        return token;
    }

    private void testInvalidConfig(String trustDomain, String bundleEndpoint) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create().providerId(SpiffeIdentityProviderFactory.PROVIDER_ID)
                .alias("another")
                .setAttribute(SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY, trustDomain)
                .setAttribute(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, bundleEndpoint).build();

        try (Response r = realm.admin().identityProviders().create(idp)) {
            Assertions.assertEquals(400, r.getStatus());
        }
    }

    public static class SpiffeServerConfig extends ClientAuthIdpServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).features(Profile.Feature.SPIFFE);
        }
    }

    public static class SpiffeIdpConfig implements OAuthIdentityProviderConfig {

        @Override
        public OAuthIdentityProviderConfigBuilder configure(OAuthIdentityProviderConfigBuilder config) {
            return config.spiffe();
        }
    }

    public static class ExernalClientAuthRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(SpiffeIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute(SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY, "mytrust-domain")
                            .setAttribute(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, "http://127.0.0.1:8500/idp/jwks")
                            .build());
        }
    }

    public static class ExernalClientAuthClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(FederatedJWTClientAuthenticator.PROVIDER_ID)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, IDP_ALIAS);
        }
    }

}
