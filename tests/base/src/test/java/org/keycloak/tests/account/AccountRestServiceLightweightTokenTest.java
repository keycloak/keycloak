/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.account;

import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.UseLightweightAccessTokenExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class AccountRestServiceLightweightTokenTest extends AccountRestServiceTest {

    private static final String POLICY_NAME = "enable lightweight tokens";

    @InjectRealm(config = LightweightTokenRealmConfig.class)
    protected ManagedRealm managedRealm;

    public static class LightweightTokenRealmConfig extends AccountRestRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            super.configure(realm);

            realm.clientProfile(ClientProfileBuilder.create()
                    .name(POLICY_NAME)
                    .description("Profile Lightweight Tokens")
                    .executor(UseLightweightAccessTokenExecutorFactory.PROVIDER_ID, null)
                    .build());

            realm.clientPolicy(ClientPolicyBuilder.create()
                    .name(POLICY_NAME)
                    .description("Policy Lightweight Tokens")
                    .condition(AnyClientConditionFactory.PROVIDER_ID, null)
                    .profile(POLICY_NAME)
                    .build());

            return realm;
        }
    }
}
