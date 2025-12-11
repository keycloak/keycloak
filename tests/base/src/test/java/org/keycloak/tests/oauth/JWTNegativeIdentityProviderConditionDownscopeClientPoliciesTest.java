/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.services.clientpolicy.condition.IdentityProviderConditionFactory;
import org.keycloak.services.clientpolicy.executor.DownscopeAssertionGrantEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
public class JWTNegativeIdentityProviderConditionDownscopeClientPoliciesTest extends JWTAuthorizationGrantDownscopeClientPoliciesTest {

    @InjectRealm(config = JWTAuthorizationGranthRealmConfig.class)
    protected ManagedRealm realm;

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
                    .condition(IdentityProviderConditionFactory.PROVIDER_ID, ClientPolicyBuilder.identityProviderConditionConfiguration(
                            true, "other"))
                    .profile("executor")
                    .build());

            return realm;
        }
    }
}
