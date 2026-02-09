package org.keycloak.tests.client.authentication.external;

import java.util.UUID;

import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.kubernetes.KubernetesIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfig;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfigBuilder;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest(config = KubernetesClientAuthTest.KubernetesServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class KubernetesClientAuthTest extends AbstractBaseClientAuthTest {

    static final String INTERNAL_CLIENT_ID = "myclient";
    static final String EXTERNAL_CLIENT_ID = "system:serviceaccount:mynamespace:myserviceaccount";
    static final String IDP_ALIAS = "kubernetes-idp";
    static final String ISSUER = "http://127.0.0.1:8500/idp";

    @InjectRealm(config = ExernalClientAuthRealmConfig.class)
    protected ManagedRealm realm;

    @InjectOAuthIdentityProvider(config = KubernetesIdpConfig.class)
    OAuthIdentityProvider identityProvider;

    public KubernetesClientAuthTest() {
        super(ISSUER, INTERNAL_CLIENT_ID, EXTERNAL_CLIENT_ID);
    }

    @Override
    protected OAuthIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    @Test
    public void testKeysCached() {
        Assertions.assertTrue(doClientGrant(createDefaultToken()).isSuccess());
        int keysRequestCount = identityProvider.getKeysRequestCount();
        Assertions.assertTrue(doClientGrant(createDefaultToken()).isSuccess());
        Assertions.assertEquals(keysRequestCount, identityProvider.getKeysRequestCount());
    }

    @Test
    public void testInvalidIssuer() {
        JsonWebToken jwt = createDefaultToken();
        jwt.issuer("https://invalid");
        assertFailure(doClientGrant(jwt));
        assertFailure(null, "https://invalid", jwt.getSubject(), jwt.getId(), "client_not_found", events.poll());
    }

    @Test
    public void testOldIAt() {
        JsonWebToken jwt = createDefaultToken();
        jwt.iat((long) (Time.currentTime() - 3550));
        assertSuccess(internalClientId, doClientGrant(jwt));
        assertSuccess(internalClientId, jwt.getId(), expectedTokenIssuer, externalClientId, events.poll());
    }

    @Test
    public void testReuse() {
        JsonWebToken jwt = createDefaultToken();
        assertSuccess(internalClientId, doClientGrant(jwt));
        assertSuccess(internalClientId, jwt.getId(), expectedTokenIssuer, externalClientId, events.poll());
        assertSuccess(internalClientId, doClientGrant(jwt));
        assertSuccess(internalClientId, jwt.getId(), expectedTokenIssuer, externalClientId, events.poll());
    }

    @Override
    protected JsonWebToken createDefaultToken() {
        JsonWebToken token = new JsonWebToken();
        token.id(UUID.randomUUID().toString());
        token.issuer(ISSUER);
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.nbf((long) (Time.currentTime()));
        token.exp((long) (Time.currentTime() + 300));
        token.iat((long) (Time.currentTime() - 300));
        token.subject(EXTERNAL_CLIENT_ID);
        return token;
    }

    public static class KubernetesServerConfig extends ClientAuthIdpServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).features(Profile.Feature.KUBERNETES_SERVICE_ACCOUNTS);
        }
    }

    public static class ExernalClientAuthRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(KubernetesIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute(IdentityProviderModel.ISSUER, ISSUER)
                            .build());

            realm.addClient(INTERNAL_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(FederatedJWTClientAuthenticator.PROVIDER_ID)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, IDP_ALIAS)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, EXTERNAL_CLIENT_ID);

            return realm;
        }
    }

    public static class KubernetesIdpConfig implements OAuthIdentityProviderConfig {

        @Override
        public OAuthIdentityProviderConfigBuilder configure(OAuthIdentityProviderConfigBuilder config) {
            return config.kubernetes();
        }
    }

}
