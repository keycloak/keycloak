package org.keycloak.tests.client.authentication.external;

import java.util.UUID;

import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = ClientAuthIdpServerConfig.class)
public class FederatedClientAuthMappingTest {

    private static final String IDP_ALIAS = "external-idp";

    @InjectRealm(config = ExernalClientAuthRealmConfig.class)
    protected ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

    @Test
    public void testSimple() {
        Assertions.assertTrue(doClientGrant(createDefaultToken("external-simple-1"), "internal-simple-1"));
        Assertions.assertTrue(doClientGrant(createDefaultToken("external-simple-2"), "internal-simple-2"));
    }

    @Test
    public void testUrn() {
        Assertions.assertTrue(doClientGrant(createDefaultToken("spiffe://client/urn"), "internal-urn"));
    }

    @Test
    public void testUri() {
        Assertions.assertTrue(doClientGrant(createDefaultToken("bf4c696e-89dc-4e40-a833-90fa5f8786e0"), "internal-uuid"));
    }

    @Test
    public void testDuplicatedExternal() {
        Assertions.assertFalse(doClientGrant(createDefaultToken("external-duplicated"), null));
    }

    private boolean doClientGrant(JsonWebToken token, String expectedInternalClientId) {
        String jws = identityProvider.encodeToken(token);
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(jws).send();
        if (response.isSuccess()) {
            AccessToken accessToken = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
            Assertions.assertEquals(expectedInternalClientId, accessToken.getIssuedFor());
        }
        return response.isSuccess();
    }

    private JsonWebToken createDefaultToken(String externalClientId) {
        JsonWebToken token = new JsonWebToken();
        token.id(UUID.randomUUID().toString());
        token.issuer("http://127.0.0.1:8500");
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.iat((long) Time.currentTime());
        token.exp((long) (Time.currentTime() + 300));
        token.subject(externalClientId);
        return token;
    }

    public static class ExernalClientAuthRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute("issuer", "http://127.0.0.1:8500")
                            .setAttribute(OIDCIdentityProviderConfig.USE_JWKS_URL, "true")
                            .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                            .setAttribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                            .setAttribute(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS, "true")
                            .build());

            createClient(realm.addClient("internal-simple-1"), "external-simple-1");
            createClient(realm.addClient("internal-simple-2"), "external-simple-2");
            createClient(realm.addClient("internal-urn"), "spiffe://client/urn");
            createClient(realm.addClient("internal-uuid"), "bf4c696e-89dc-4e40-a833-90fa5f8786e0");
            createClient(realm.addClient("internal-duplicated-1"), "external-duplicated");
            createClient(realm.addClient("internal-duplicated-2"), "external-duplicated");

            return realm;
        }

        private static void createClient(ClientConfigBuilder client, String externalId) {
            client.serviceAccountsEnabled(true)
                    .authenticatorType(FederatedJWTClientAuthenticator.PROVIDER_ID)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, IDP_ALIAS)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, externalId);
        }

    }

}
