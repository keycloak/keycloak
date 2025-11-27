package org.keycloak.tests.oauth;

import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.JWTClaimEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.JWTClaimEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
public class JWTAuthorizationGrantJWTClaimsClientPoliciesTest extends BaseAbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void testClaimPresenceOnly() {
        updateExecutorConfig("username", null);

        JsonWebToken assertionJwt = createDefaultAuthorizationGrantToken();

        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertFailurePolicy("invalid_request", "Required claim 'username' is missing from the token", response, events.poll());

        assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("username", "anyvalue");
        response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testSubjectClaimExactMatch() {
        updateExecutorConfig("sub", List.of("basic-user-id-1"));

        JsonWebToken assertionJwt = createDefaultAuthorizationGrantToken();
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertFailurePolicy("invalid_request", "Claim 'sub' not allowed", response, events.poll());
    }

    @Test
    public void testClaimExactMatch() {
        updateExecutorConfig("username", List.of("test-username"));

        JsonWebToken assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("username", "test-username");

        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertSuccess("test-app", response);

        assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("username", "wronguser");
        response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertFailurePolicy("invalid_request", "Claim 'username' not allowed", response, events.poll());
    }

    @Test
    public void testClaimWildcardMatch() {
        updateExecutorConfig("username", List.of("test-username*"));

        JsonWebToken assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("username", "test-username123");

        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertSuccess("test-app", response);

        assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("username", "wronguser");
        response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertFailurePolicy("invalid_request", "Claim 'username' not allowed", response, events.poll());
    }

    @Test
    public void testClaimNumberMatch() {
        updateExecutorConfig("level", List.of("3", "5", "6*"));

        JsonWebToken assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("level", 3);

        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertSuccess("test-app", response);

        assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("level", 61);

        response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertSuccess("test-app", response);

        assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("level", 2);
        response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertFailurePolicy("invalid_request", "Claim 'level' not allowed", response, events.poll());
    }

    @Test
    public void testClaimNowAllowedType() {
        updateExecutorConfig("username",  List.of("test-username"));

        JsonWebToken assertionJwt = createDefaultAuthorizationGrantToken();
        assertionJwt.getOtherClaims().put("username", Map.of("test-username","asdf"));

        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(identityProvider.encodeToken(assertionJwt)).send();
        assertFailurePolicy("invalid_request", "Claim value is not allowed", response, events.poll());
    }

    protected void updateExecutorConfig(String claimName, List<String> allowedValues) {
        JWTClaimEnforcerExecutor.Configuration claimsConfig = new JWTClaimEnforcerExecutor.Configuration();
        claimsConfig.setClaimName(claimName);
        claimsConfig.setAllowedValues(allowedValues);
        updateExecutorConfig(claimsConfig);
    }
    protected void updateExecutorConfig(JWTClaimEnforcerExecutor.Configuration newConfig) {

        realm.updateWithCleanup(r -> {

            JWTClaimEnforcerExecutor.Configuration claimsConfig = new JWTClaimEnforcerExecutor.Configuration();
            claimsConfig.setClaimName("username");
            claimsConfig.setAllowedValues(List.of("test-username"));
            r.clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(JWTClaimEnforcerExecutorFactory.PROVIDER_ID, newConfig)
                    .build());

            r.clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(GrantTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.grantTypeConditionConfiguration(
                            OAuth2Constants.JWT_AUTHORIZATION_GRANT))
                    .profile("executor")
                    .build());

            return r;
        });
    }

    public static class JWTAuthorizationGrantRealmConfig extends OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            super.configure(realm);
            return realm;
        }
    }
}
