package org.keycloak.tests.oauth;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest(config = OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class OIDCIdentityProviderJWTAuthorizationGrantTest extends AbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void testNotAllowedIdentityProvider() {
        realm.updateIdentityProviderWithCleanup(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ENABLED, "false");
        });

        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        EventAssertion event = assertFailure("JWT Authorization Granted is not enabled for the identity provider", response, events.poll());
        event.details(Details.IDENTITY_PROVIDER, IDP_ALIAS);
        event.details(Details.IDENTITY_PROVIDER_ISSUER, IDP_ISSUER);
        event.details(Details.IDENTITY_PROVIDER_USER_ID, "basic-user-id");
    }

    @Test
    public void testValidateSignatureDisabled() {
        realm.updateIdentityProviderWithCleanup(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "false");
        });

        // Test with JWT signed by invalid key. It tests that signature validation is triggered even if "validate signature" switch on OIDC provider is false
        testInvalidSignature();

        // Test with correct signature
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    public static class JWTAuthorizationGrantRealmConfig extends AbstractJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            super.configure(realm);
            realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute(IdentityProviderModel.ISSUER, IDP_ISSUER)
                            .setAttribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString())
                            .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                            .setAttribute(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.TRUE.toString())
                            .setAttribute(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString())
                            .build());
            return realm;
        }
    }
}
