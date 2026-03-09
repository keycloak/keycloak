package org.keycloak.tests.oauth;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class OIDCIdentityProviderJWTAuthorizationGrantTest extends AbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void testNotAllowedIdentityProvider() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ENABLED, "false");
        });

        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        EventAssertion event = assertFailure("No Identity Provider for provided issuer", response, events.poll());
        event.details(Details.IDENTITY_PROVIDER_ISSUER, IDP_ISSUER);
        event.details(Details.IDENTITY_PROVIDER_USER_ID, "basic-user-id");
    }

    @Test
    public void testValidateSignatureDisabled() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "false");
        });

        // Test with JWT signed by invalid key. It tests that signature validation is triggered even if "validate signature" switch on OIDC provider is false
        testInvalidSignature();

        // Test with correct signature
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testClientIdAllowedAsAudience() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.ALLOW_CLIENT_ID_AS_AUDIENCE, Boolean.TRUE.toString());
        });

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());
    }

    @Test
    public void testCustomAudiencesWithClientId() {
        // set attribute for custom audiences
        ClientResource clientResource = AdminApiUtil.findClientByClientId(realm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c -> c.attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE,
                String.format("[{\"key\":\"%s\",\"value\":\"allowed-aud1\"},{\"key\":\"%s\",\"value\":\"allowed-aud2\"}]", IDP_ALIAS, IDP_ALIAS)));

        // update to use client-id
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.ALLOW_CLIENT_ID_AS_AUDIENCE, Boolean.TRUE.toString());
        });

        // test normal client-id is not working anymore
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        // test allowed-aud1 is valid
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "allowed-aud1", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        // test allowed-aud2 is valid
        jwt = identityProvider.encodeToken(createAuthorizationGrantToken("basic-user-id", "allowed-aud2", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        // test any other audience is wrong
        jwt = identityProvider.encodeToken(createAuthorizationGrantToken("basic-user-id", "other-aud", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        // test issuer audience is wrong
        jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        // test two audiences are always wrong
        JsonWebToken jwtToken = createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER);
        jwtToken.addAudience("allowed-aud2");
        jwt = getIdentityProvider().encodeToken(jwtToken);
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Multiple audiences not allowed", response, events.poll());
    }

    public static class JWTAuthorizationGrantRealmConfig extends AbstractJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            super.configure(realm);
            realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute("clientId", "test-client")
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
