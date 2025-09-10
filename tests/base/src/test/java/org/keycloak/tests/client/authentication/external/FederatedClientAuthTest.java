package org.keycloak.tests.client.authentication.external;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.UUID;

@KeycloakIntegrationTest(config = ClientAuthIdpServerConfig.class)
public class FederatedClientAuthTest {

    private static final String IDP_ALIAS = "external-idp";

    private static final String CLIENT_ID = "myclient";

    @InjectRealm(config = ExernalClientAuthRealmConfig.class)
    protected ManagedRealm realm;

    @InjectClient(config = ExernalClientAuthClientConfig.class)
    protected ManagedClient client;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

    @Test
    public void testInvalidSignature() {
        OAuthIdentityProvider.OAuthIdentityProviderKeys keys = identityProvider.createKeys();
        String jws = identityProvider.encodeToken(createDefaultToken(), keys);
        Assertions.assertFalse(doClientGrant(jws));
    }

    @Test
    public void testInvalidAssertionType() {
        String jws = identityProvider.encodeToken(createDefaultToken());
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(jws, "urn:ietf:params:oauth:client-assertion-type:jwt-spiffe").send();
        Assertions.assertFalse(response.isSuccess());
    }

    @Test
    public void testValidToken() {
        Assertions.assertTrue(doClientGrant(createDefaultToken()));
    }

    @Test
    public void testInvalidIssuer() {
        JsonWebToken token = createDefaultToken();
        token.issuer("http://invalid");
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testMissingIssuer() {
        JsonWebToken token = createDefaultToken();
        token.issuer(null);
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testInvalidSub() {
        JsonWebToken token = createDefaultToken();
        token.subject("invalid");
        Assertions.assertFalse(doClientGrant(token));
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
        token.nbf((long) (Time.currentTime() + 30));
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testMissingJti() {
        JsonWebToken token = createDefaultToken();
        token.id(null);
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
    public void testReuseNotPermitted() {
        JsonWebToken token = createDefaultToken();
        Assertions.assertTrue(doClientGrant(token));
        Assertions.assertFalse(doClientGrant(token));
    }

    @Test
    public void testReusePermitted() {
        IdentityProviderResource idp = realm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().put(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTION_REUSE, "true");
        idp.update(rep);

        JsonWebToken token = createDefaultToken();
        Assertions.assertTrue(doClientGrant(token));
        Assertions.assertTrue(doClientGrant(token));

        rep.getConfig().remove(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTION_REUSE);
        idp.update(rep);
    }

    @Test
    public void testClientAssertionsNotSupported() {
        IdentityProviderResource idp = realm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().remove(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS);
        idp.update(rep);

        Assertions.assertFalse(doClientGrant(createDefaultToken()));

        rep.getConfig().put(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS, "true");
        idp.update(rep);
    }

    private boolean doClientGrant(JsonWebToken token) {
        String jws = identityProvider.encodeToken(token);
        return doClientGrant(jws);
    }

    private boolean doClientGrant(String jws) {
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(jws).send();
        return response.isSuccess();
    }

    private JsonWebToken createDefaultToken() {
        JsonWebToken token = new JsonWebToken();
        token.id(UUID.randomUUID().toString());
        token.issuer("http://127.0.0.1:8500");
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.iat((long) Time.currentTime());
        token.exp((long) (Time.currentTime() + 300));
        token.subject(CLIENT_ID);
        return token;
    }

    public static class ExernalClientAuthRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute("issuer", "http://127.0.0.1:8500")
                            .setAttribute(OIDCIdentityProviderConfig.USE_JWKS_URL, "true")
                            .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                            .setAttribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                            .setAttribute(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS, "true")
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
