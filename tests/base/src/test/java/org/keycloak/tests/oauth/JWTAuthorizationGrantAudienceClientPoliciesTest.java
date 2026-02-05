/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.oauth;

import java.util.Set;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.clientpolicy.condition.IdentityProviderConditionFactory;
import org.keycloak.services.clientpolicy.executor.JWTAuthorizationGrantAudienceExecutor;
import org.keycloak.services.clientpolicy.executor.JWTAuthorizationGrantAudienceExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
public class JWTAuthorizationGrantAudienceClientPoliciesTest extends BaseAbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = JWTAuthorizationGrantAudienceClientPoliciesTest.JWTAuthorizationGranthRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void testAudiences() {
        // test normal issuer audience is not valid
        String jwt = identityProvider.encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_grant", "Invalid token audience", response, events.poll());

        // test allowed-aud1 is valid
        jwt = identityProvider.encodeToken(createAuthorizationGrantToken("basic-user-id", "allowed-aud1", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        // test allowed-aud2 is valid
        jwt = identityProvider.encodeToken(createAuthorizationGrantToken("basic-user-id", "allowed-aud2", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        // test any other audience is wrong
        jwt = identityProvider.encodeToken(createAuthorizationGrantToken("basic-user-id", "other-aud", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_grant", "Invalid token audience", response, events.poll());

        // test client-id audience is wrong
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_grant", "Invalid token audience", response, events.poll());

        // test two audiences are always wrong
        JsonWebToken jwtToken = createDefaultAuthorizationGrantToken();
        jwtToken.addAudience("allowed-aud2");
        jwt = getIdentityProvider().encodeToken(jwtToken);
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Multiple audiences not allowed", response, events.poll());
    }

    @Test
    public void testAudiencesWithClientId() {
        // update to use client-id
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.ALLOW_CLIENT_ID_AS_AUDIENCE, Boolean.TRUE.toString());
        });

        // test normal client-id is not working anymore
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_grant", "Invalid token audience", response, events.poll());

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
        assertFailurePolicy("invalid_grant", "Invalid token audience", response, events.poll());

        // test issuer audience is wrong
        jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailurePolicy("invalid_grant", "Invalid token audience", response, events.poll());

        // test two audiences are always wrong
        JsonWebToken jwtToken = createAuthorizationGrantToken("basic-user-id", "test-client", IDP_ISSUER);
        jwtToken.addAudience("allowed-aud2");
        jwt = getIdentityProvider().encodeToken(jwtToken);
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Multiple audiences not allowed", response, events.poll());
    }

    public static class JWTAuthorizationGranthRealmConfig extends OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            super.configure(realm);
            JWTAuthorizationGrantAudienceExecutor.Configuration config =
                    new JWTAuthorizationGrantAudienceExecutor.Configuration();
            config.setAllowedAudience(Set.of("allowed-aud1", "allowed-aud2"));

            realm.clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(JWTAuthorizationGrantAudienceExecutorFactory.PROVIDER_ID, config)
                    .build());

            realm.clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(IdentityProviderConditionFactory.PROVIDER_ID, ClientPolicyBuilder
                            .identityProviderConditionConfiguration(false, IDP_ALIAS))
                    .profile("executor")
                    .build());

            return realm;
        }
    }
}
