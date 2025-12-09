package org.keycloak.tests.oauth;

import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.DownscopeAssertionGrantEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
public class JWTAuthorizationGrantDownscopeClientPoliciesTest extends BaseAbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = JWTAuthorizationGranthRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void testDownscope() throws Exception {
        // test with all the scopes
        String jwt = identityProvider.encodeToken(createDefaultAuthorizationGrantToken("email profile address"));
        AccessTokenResponse response = oAuthClient.openid(false).scope("address").jwtAuthorizationGrantRequest(jwt).send();
        AccessToken token = assertSuccess("test-app", response);
        MatcherAssert.assertThat(List.of(token.getScope().split(" ")), Matchers.containsInAnyOrder("email", "profile", "address"));

        // test with less scopes => downscope
        jwt = identityProvider.encodeToken(createDefaultAuthorizationGrantToken("email profile address"));
        response = oAuthClient.openid(false).scope(null).jwtAuthorizationGrantRequest(jwt).send();
        token = assertSuccess("test-app", response);
        MatcherAssert.assertThat(List.of(token.getScope().split(" ")), Matchers.containsInAnyOrder("email", "profile"));

        // test default scopes are restricted if not present in initial token
        jwt = identityProvider.encodeToken(createDefaultAuthorizationGrantToken("profile address"));
        response = oAuthClient.openid(false).scope("address").jwtAuthorizationGrantRequest(jwt).send();
        token = assertSuccess("test-app", response);
        MatcherAssert.assertThat(List.of(token.getScope().split(" ")), Matchers.containsInAnyOrder("profile", "address"));

        // test requesting a valid optional scope for the client but not present initially
        jwt = identityProvider.encodeToken(createDefaultAuthorizationGrantToken("email profile"));
        response = oAuthClient.openid(false).scope("address").jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_scope", "Scopes [address] not present in the initial access token [profile, email]", response, events.poll());

        // test requesting a default scope not present in the initial token
        jwt = identityProvider.encodeToken(createDefaultAuthorizationGrantToken("email address"));
        response = oAuthClient.openid(false).scope("email profile address").jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_scope", "Scopes [profile] not present in the initial access token [address, email]", response, events.poll());
    }

    public static class JWTAuthorizationGranthRealmConfig extends OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            super.configure(realm);

            realm.clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(DownscopeAssertionGrantEnforcerExecutorFactory.PROVIDER_ID, null)
                    .build());

            realm.clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(GrantTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.grantTypeConditionConfiguration(
                            false, OAuth2Constants.JWT_AUTHORIZATION_GRANT))
                    .profile("executor")
                    .build());

            return realm;
        }
    }
}
