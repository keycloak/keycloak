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

import org.keycloak.OAuth2Constants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.UseLightweightAccessTokenExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class JWTAuthorizationGrantLightweightAccessTokenClientPoliciesTest extends BaseAbstractJWTAuthorizationGrantTest {

    private static final String HEAVY_CLAIM = "heavy_claim";

    @InjectRealm(config = JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void testUseLightweightAccessTokenExecutor() {
        realm.updateClientWithCleanup("test-app", client -> client
                .protocolMappers(ModelToRepresentation.toRepresentation(HardcodedClaim.create(
                        HEAVY_CLAIM,
                        HEAVY_CLAIM,
                        "heavy-value",
                        "String",
                        true,
                        false,
                        false))));

        String assertion = identityProvider.encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.openid(false).jwtAuthorizationGrantRequest(assertion).send();
        Assertions.assertTrue(response.isSuccess(), response.getErrorDescription());

        AccessToken lightweightToken = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        Assertions.assertEquals("test-app", lightweightToken.getIssuedFor());
        Assertions.assertNull(lightweightToken.getPreferredUsername());
        Assertions.assertFalse(lightweightToken.getOtherClaims().containsKey(HEAVY_CLAIM));
    }

    public static class JWTAuthorizationGrantRealmConfig extends OIDCIdentityProviderJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            super.configure(realm);

            realm.clientProfile(ClientProfileBuilder.create()
                    .name("lightweight-token-profile")
                    .description("Use lightweight access tokens")
                    .executor(UseLightweightAccessTokenExecutorFactory.PROVIDER_ID, null)
                    .build());

            realm.clientPolicy(ClientPolicyBuilder.create()
                    .name("lightweight-token-policy")
                    .description("Use lightweight access tokens for JWT Authorization Grant requests")
                    .condition(GrantTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.grantTypeConditionConfiguration(
                            false, OAuth2Constants.JWT_AUTHORIZATION_GRANT))
                    .profile("lightweight-token-profile")
                    .build());

            return realm;
        }
    }
}
