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
package org.keycloak.tests.client;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 *
 * @author rmartinc
 */
public class ClientSecretRotationUtils {

    private static final String PROFILE_NAME = "ClientSecretRotationProfile";
    private static final String POLICY_NAME = "ClientSecretRotationPolicy";

    public static final int DEFAULT_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.HOURS.toSeconds(1)).intValue();
    public static final int DEFAULT_ROTATED_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(10)).intValue();
    public static final int DEFAULT_REMAIN_EXPIRATION_PERIOD = Long.valueOf(TimeUnit.MINUTES.toSeconds(30)).intValue();

    public static class ClientSecretRotationServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_SECRET_ROTATION);
        }
    }

    private ClientSecretRotationUtils() {
        // utility class
    }

    public static ClientSecretRotationExecutor.Configuration getClientProfileConfiguration(
            long expirationPeriod, long rotatedExpirationPeriod, long remainExpirationPeriod) {
        ClientSecretRotationExecutor.Configuration profileConfig = new ClientSecretRotationExecutor.Configuration();
        profileConfig.setExpirationPeriod(expirationPeriod);
        profileConfig.setRotatedExpirationPeriod(rotatedExpirationPeriod);
        profileConfig.setRemainExpirationPeriod(remainExpirationPeriod);
        return profileConfig;
    }

    public static void doConfigProfile(ManagedRealm realm, ClientSecretRotationExecutor.Configuration profileConfig) {
        ClientProfileRepresentation clientProfile = ClientProfileBuilder.create()
                .name(PROFILE_NAME)
                .description("Enable Client Secret Rotation")
                .executor(ClientSecretRotationExecutorFactory.PROVIDER_ID, profileConfig)
                .build();
        ClientProfilesRepresentation clientProfiles = new ClientProfilesRepresentation();
        clientProfiles.setProfiles(List.of(clientProfile));
        realm.admin().clientPoliciesProfilesResource().updateProfiles(clientProfiles);
    }

    public static void configureDefaultProfileAndPolicy(ManagedRealm realm) {
        configureCustomProfileAndPolicy(realm, DEFAULT_EXPIRATION_PERIOD,
                DEFAULT_ROTATED_EXPIRATION_PERIOD, DEFAULT_REMAIN_EXPIRATION_PERIOD);
    }

    public static void configureCustomProfileAndPolicy(ManagedRealm realm, long secretExpiration, long rotatedExpiration, long remainingExpiration) {
        realm.updateWithCleanup(r -> {
            r.clientProfile(ClientProfileBuilder.create()
                    .name(PROFILE_NAME)
                    .description("Enable Client Secret Rotation")
                    .executor(ClientSecretRotationExecutorFactory.PROVIDER_ID, getClientProfileConfiguration(
                            secretExpiration, rotatedExpiration, remainingExpiration))
                    .build());

            r.clientPolicy(ClientPolicyBuilder.create()
                    .name(POLICY_NAME)
                    .description("Policy for Client Secret Rotation")
                    .condition(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientAccessTypeCondition(
                            false, ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL))
                    .profile(PROFILE_NAME)
                    .build());

            return r;
        });
    }

    public static void disableProfile(ManagedRealm realm) {
        realm.updateWithCleanup(r -> {
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                            .name(POLICY_NAME)
                            .description("Policy for Client Secret Rotation")
                            .condition(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientPolicyBuilder.clientAccessTypeCondition(
                                    false, ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL))
                            .profile(PROFILE_NAME)
                            .enabled(false)
                            .build());

            return r;
        });
    }
}
