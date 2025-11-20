package org.keycloak.tests.client.authentication.external;

import java.util.UUID;

import org.keycloak.authentication.authenticators.client.FederatedJWTClientAuthenticator;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = ClientAuthIdpServerConfig.class)
public class BaseClientAuthTest extends AbstractBaseClientAuthTest {

    private static final String IDP_ALIAS = "external-idp";

    private static final String TOKEN_ISSUER = "http://127.0.0.1:8500";
    private static final String INTERNAL_CLIENT_ID = "internal-myclient";
    private static final String EXTERNAL_CLIENT_ID = "external-myclient";

    @InjectRealm(config = ExernalClientAuthRealmConfig.class)
    protected ManagedRealm realm;

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

    public BaseClientAuthTest() {
        super(TOKEN_ISSUER, INTERNAL_CLIENT_ID, EXTERNAL_CLIENT_ID);
    }

    @Test
    public void testInvalidIssuer() {
        JsonWebToken jwt = createDefaultToken();
        jwt.issuer("http://invalid");

        assertFailure("Invalid client or Invalid client credentials", doClientGrant(jwt));
        assertFailure(null, "http://invalid", jwt.getSubject(), jwt.getId(), "client_not_found", events.poll());
    }

    @Test
    public void testMissingIssuer() {
        JsonWebToken jwt = createDefaultToken();
        jwt.issuer(null);
        Assertions.assertFalse(doClientGrant(jwt).isSuccess());
        assertFailure(null, null, jwt.getSubject(), jwt.getId(), "client_not_found", events.poll());
    }

    @Test
    public void testMissingJti() {
        JsonWebToken jwt = createDefaultToken();
        jwt.id(null);
        Assertions.assertFalse(doClientGrant(jwt).isSuccess());
        assertFailure(INTERNAL_CLIENT_ID, TOKEN_ISSUER, jwt.getSubject(), jwt.getId(), events.poll());
    }

    @Test
    public void testReuseNotPermitted() {
        JsonWebToken jwt = createDefaultToken();
        assertSuccess(INTERNAL_CLIENT_ID, doClientGrant(jwt));
        assertSuccess(INTERNAL_CLIENT_ID, jwt.getId(), TOKEN_ISSUER, EXTERNAL_CLIENT_ID, events.poll());
        assertFailure("Token reuse detected", doClientGrant(jwt));
        assertFailure(INTERNAL_CLIENT_ID, TOKEN_ISSUER, EXTERNAL_CLIENT_ID, jwt.getId(), events.poll());
    }

    @Test
    public void testReusePermitted() {
        realm.updateIdentityProviderWithCleanup(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTION_REUSE, "true");
        });

        JsonWebToken jwt = createDefaultToken();
        assertSuccess(INTERNAL_CLIENT_ID, doClientGrant(jwt));
        assertSuccess(INTERNAL_CLIENT_ID, jwt.getId(), TOKEN_ISSUER, EXTERNAL_CLIENT_ID, events.poll());
        assertSuccess(INTERNAL_CLIENT_ID, doClientGrant(jwt));
        assertSuccess(INTERNAL_CLIENT_ID, jwt.getId(), TOKEN_ISSUER, EXTERNAL_CLIENT_ID, events.poll());
    }

    @Test
    public void testClientAssertionsNotSupported() {
        realm.updateIdentityProviderWithCleanup(IDP_ALIAS, rep -> {
            rep.getConfig().remove(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS);
        });

        JsonWebToken jwt = createDefaultToken();
        assertFailure(doClientGrant(jwt));
        assertFailure(null, TOKEN_ISSUER, EXTERNAL_CLIENT_ID, jwt.getId(), "client_not_found", events.poll());
    }

    @Override
    protected OAuthIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    protected JsonWebToken createDefaultToken() {
        JsonWebToken token = new JsonWebToken();
        token.id(UUID.randomUUID().toString());
        token.issuer("http://127.0.0.1:8500");
        token.audience(oAuthClient.getEndpoints().getIssuer());
        token.iat((long) Time.currentTime());
        token.exp((long) (Time.currentTime() + 300));
        token.subject(EXTERNAL_CLIENT_ID);
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

            realm.addClient(INTERNAL_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(FederatedJWTClientAuthenticator.PROVIDER_ID)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_ISSUER_KEY, IDP_ALIAS)
                    .attribute(FederatedJWTClientAuthenticator.JWT_CREDENTIAL_SUBJECT_KEY, EXTERNAL_CLIENT_ID);

            return realm;
        }
    }

}
